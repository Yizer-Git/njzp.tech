# -*- coding: utf-8 -*-
# @Time : 2024-12-2024/12/26 23:21
# @Author : 农疾智判
# @File : main.py

import json
import os
import subprocess
import threading
import time
import uuid
import cv2
import numpy as np
import requests
import torch
from flask import Flask, Response, request
from ultralytics import YOLO
from urllib.parse import urlparse, urljoin
from predict.predictImg import ImagePredictor
from flask_socketio import SocketIO, emit


# Flask 应用设置
class VideoProcessingApp:
    def __init__(self, host='0.0.0.0', port=5001):
        """初始化 Flask 应用并设置路由"""
        self.app = Flask(__name__)
        self.socketio = SocketIO(self.app, cors_allowed_origins="*")  # 初始化 SocketIO
        self.host = host
        self.port = port
        self.setup_routes()
        self.data = {}  # 存储接收参数
        self.local_temp_dir = os.path.join(self.app.root_path, 'runs', 'temp')
        self.result_dir = os.path.join(self.app.root_path, 'static', 'results')
        os.makedirs(self.local_temp_dir, exist_ok=True)
        os.makedirs(self.result_dir, exist_ok=True)
        self.enable_remote_upload = os.getenv("ENABLE_REMOTE_UPLOAD", "1") == "1"
        self.paths = {
            'download': './runs/video/download.mp4',
            'output': './runs/video/output.mp4',
            'camera_output': "./runs/video/camera_output.avi",
            'video_output': "./runs/video/camera_output.avi"
        }
        self.device = "cuda:0" if torch.cuda.is_available() else "cpu"
        self.use_half = self.device != "cpu"
        self.model_cache = {}
        self.model_ready = set()
        self.recording = False  # 标志位，判断是否正在录制视频

    def setup_routes(self):
        """设置所有路由"""
        self.app.add_url_rule('/file_names', 'file_names', self.file_names, methods=['GET'])
        self.app.add_url_rule('/predictImg', 'predictImg', self.predictImg, methods=['POST'])
        self.app.add_url_rule('/predictVideo', 'predictVideo', self.predictVideo)
        self.app.add_url_rule('/predictCamera', 'predictCamera', self.predictCamera)
        self.app.add_url_rule('/stopCamera', 'stopCamera', self.stopCamera, methods=['GET'])

        # 添加 WebSocket 事件
        @self.socketio.on('connect')
        def handle_connect():
            print("WebSocket connected!")
            emit('message', {'data': 'Connected to WebSocket server!'})

        @self.socketio.on('disconnect')
        def handle_disconnect():
            print("WebSocket disconnected!")

    def run(self):
        """启动 Flask 应用"""
        self.socketio.run(self.app, host=self.host, port=self.port, allow_unsafe_werkzeug=True)

    def file_names(self):
        """模型列表接口"""
        weight_items = [{'value': name, 'label': name} for name in self.get_file_names("./weights")]
        return json.dumps({'weight_items': weight_items})

    def predictImg(self):
        """图片预测接口"""
        data = request.get_json()
        print(data)
        self.data.clear()
        self.data.update({
            "username": data['username'], "weight": data['weight'],
            "conf": data['conf'], "startTime": data['startTime'],
            "inputImg": data['inputImg'],
            "kind": data['kind'],
            "taskId": data.get('taskId')
        })
        print(self.data)
        task_id = self.data.get("taskId")
        self.emit_task_event(task_id, 'processing', message='图片识别中', username=self.data.get("username"), kind=self.data.get("kind"))

        input_img = self.data["inputImg"]
        cleanup_required = False
        if self.is_remote_resource(input_img):
            parsed = urlparse(input_img)
            img_filename = os.path.basename(parsed.path) or f"remote_{uuid.uuid4().hex}.jpg"
            local_img_path = os.path.join(self.local_temp_dir, img_filename)
            self.download(input_img, local_img_path)
            if not os.path.exists(local_img_path):
                self.data["status"] = 400
                self.data["message"] = "图片下载失败，请稍后重试！"
                return json.dumps(self.data, ensure_ascii=False)
            cleanup_required = True
        else:
            local_img_path = self.resolve_local_image_path(input_img)
            if not os.path.exists(local_img_path):
                self.data["status"] = 400
                self.data["message"] = "提供的本地图片路径不存在，请重新上传！"
                self.emit_task_event(task_id, 'failed', message=self.data["message"])
                return json.dumps(self.data, ensure_ascii=False)

        result_filename = f"result_{uuid.uuid4().hex}.jpg"
        result_path = os.path.join(self.result_dir, result_filename)
        os.makedirs(os.path.dirname(result_path), exist_ok=True)

        model, load_duration = self.get_or_load_model(self.data["weight"])
        predict = ImagePredictor(weights_path=f'./weights/{self.data["weight"]}',
                                 img_path=local_img_path, save_path=result_path, kind=self.data["kind"],
                                 conf=float(self.data["conf"]), device=self.device, model=model)
        # 执行预测
        results = predict.predict(setup_time=load_duration)

        if results['labels'] != '预测失败':
            self.data["status"] = 200
            self.data["message"] = "预测成功"
            result_url = urljoin(request.host_url, f"static/results/{result_filename}")
            self.data["outImg"] = result_url
            self.data["allTime"] = results['allTime']
            self.data["confidence"] = json.dumps(results['confidences'])
            self.data["label"] = json.dumps(results['labels'])
            self.emit_task_event(
                task_id,
                'completed',
                outImg=result_url,
                labels=results['labels'],
                confidences=results['confidences'],
                allTime=results['allTime']
            )
            if self.enable_remote_upload:
                self.data["uploadStatus"] = "pending"
                self.schedule_async_upload(result_path, result_filename)
            else:
                self.data["uploadStatus"] = "skipped"
        else:
            self.data["status"] = 400
            self.data["message"] = "该图片无法识别，请重新上传！"
            self.emit_task_event(task_id, 'failed', message=self.data["message"])
            if os.path.exists(result_path):
                os.remove(result_path)
        
        # 清理临时文件
        if cleanup_required and os.path.exists(local_img_path):
            os.remove(local_img_path)
        
        return json.dumps(self.data, ensure_ascii=False)

    def predictVideo(self):
        """视频流处理接口"""
        self.data.clear()
        self.data.update({
            "username": request.args.get('username'), "weight": request.args.get('weight'),
            "conf": request.args.get('conf'), "startTime": request.args.get('startTime'),
            "inputVideo": request.args.get('inputVideo'),
            "kind": request.args.get('kind'),
            "taskId": request.args.get('taskId')
        })
        task_id = self.data.get("taskId")
        self.emit_task_event(task_id, 'processing', message='视频识别处理中', username=self.data.get("username"))
        self.download(self.data["inputVideo"], self.paths['download'])
        cap = cv2.VideoCapture(self.paths['download'])
        if not cap.isOpened():
            self.emit_task_event(task_id, 'failed', message='无法打开视频文件')
            raise ValueError("无法打开视频文件")
        fps = int(cap.get(cv2.CAP_PROP_FPS))
        print(fps)

        # 视频写入器
        video_writer = cv2.VideoWriter(
            self.paths['video_output'],
            cv2.VideoWriter_fourcc(*'XVID'),
            fps,
            (640, 480)
        )
        model, _ = self.get_or_load_model(self.data["weight"])

        def generate():
            try:
                while cap.isOpened():
                    ret, frame = cap.read()
                    if not ret:
                        break
                    frame = cv2.resize(frame, (640, 480))
                    results = model.predict(
                        source=frame,
                        conf=float(self.data['conf']),
                        show=False,
                        device=self.device,
                        half=self.use_half
                    )
                    processed_frame = results[0].plot()
                    video_writer.write(processed_frame)
                    _, jpeg = cv2.imencode('.jpg', processed_frame)
                    yield b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'
            finally:
                self.cleanup_resources(cap, video_writer)
                self.socketio.emit('message', {'data': '处理完成，正在保存！'})
                for progress in self.convert_avi_to_mp4(self.paths['video_output']):
                    self.socketio.emit('progress', {'data': progress})
                uploadedUrl = self.upload(self.paths['output'])
                self.data["outVideo"] = uploadedUrl
                self.save_data(json.dumps(self.data), 'http://localhost:9999/videoRecords')
                self.emit_task_event(task_id, 'completed', outVideo=uploadedUrl, username=self.data.get("username"))
                self.cleanup_files([self.paths['download'], self.paths['output'], self.paths['video_output']])

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

    def predictCamera(self):
        """摄像头视频流处理接口"""
        self.data.clear()
        self.data.update({
            "username": request.args.get('username'), "weight": request.args.get('weight'),
            "kind": request.args.get('kind'),
            "conf": request.args.get('conf'), "startTime": request.args.get('startTime')
        })
        self.socketio.emit('message', {'data': '正在加载，请稍等！'})
        weight_name = self.data["weight"]
        if not weight_name:
            warning = '请先选择模型再开始摄像头预测。'
            self.socketio.emit('message', {'data': warning})
            self.socketio.emit('camera_status', {'status': 'error', 'message': warning})
            return Response(status=400)

        model, _ = self.get_or_load_model(weight_name)
        cap, camera_index, diagnostics = self.open_camera()
        if diagnostics:
            self.socketio.emit('camera_status', {
                'status': 'probing',
                'attempts': diagnostics
            })
        if cap is None:
            warning = '未检测到可用摄像头，请检查设备连接。'
            self.socketio.emit('message', {'data': warning})
            self.socketio.emit('camera_status', {'status': 'error', 'message': warning})
            return Response(status=503)

        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        success_msg = f'摄像头已连接 (索引 {camera_index})，开始识别。'
        self.socketio.emit('message', {'data': success_msg})
        self.socketio.emit('camera_status', {'status': 'streaming', 'index': camera_index})
        video_writer = cv2.VideoWriter(self.paths['camera_output'], cv2.VideoWriter_fourcc(*'XVID'), 20, (640, 480))
        self.recording = True

        def generate():
            try:
                while self.recording:
                    ret, frame = cap.read()
                    if not ret:
                        break
                    results = model.predict(
                        source=frame,
                        imgsz=640,
                        conf=float(self.data['conf']),
                        show=False,
                        device=self.device,
                        half=self.use_half
                    )
                    processed_frame = results[0].plot()
                    if self.recording and video_writer:
                        video_writer.write(processed_frame)
                    _, jpeg = cv2.imencode('.jpg', processed_frame)
                    yield b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'
            finally:
                self.cleanup_resources(cap, video_writer)
                self.socketio.emit('message', {'data': '处理完成，正在保存！'})
                self.socketio.emit('camera_status', {'status': 'stopped'})
                for progress in self.convert_avi_to_mp4(self.paths['camera_output']):
                    self.socketio.emit('progress', {'data': progress})
                uploadedUrl = self.upload(self.paths['output'])
                self.data["outVideo"] = uploadedUrl
                print(self.data)
                self.save_data(json.dumps(self.data), 'http://localhost:9999/cameraRecords')
                self.cleanup_files([self.paths['download'], self.paths['output'], self.paths['camera_output']])

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

    def stopCamera(self):
        """停止摄像头预测"""
        self.recording = False
        self.socketio.emit('camera_status', {'status': 'stopped'})
        return json.dumps({"status": 200, "message": "预测成功", "code": 0})

    def save_data(self, data, path):
        """将结果数据上传到服务器"""
        headers = {'Content-Type': 'application/json'}
        try:
            response = requests.post(path, data=data, headers=headers)
            print("记录上传成功！" if response.status_code == 200 else f"记录上传失败，状态码: {response.status_code}")
        except requests.RequestException as e:
            print(f"上传记录时发生错误: {str(e)}")

    def convert_avi_to_mp4(self, temp_output):
        """使用 FFmpeg 将 AVI 格式转换为 MP4 格式，并显示转换进度。"""
        ffmpeg_command = f"ffmpeg -i {temp_output} -vcodec libx264 {self.paths['output']} -y"
        process = subprocess.Popen(ffmpeg_command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   text=True)
        total_duration = self.get_video_duration(temp_output)

        for line in process.stderr:
            if "time=" in line:
                try:
                    time_str = line.split("time=")[1].split(" ")[0]
                    h, m, s = map(float, time_str.split(":"))
                    processed_time = h * 3600 + m * 60 + s
                    if total_duration > 0:
                        progress = (processed_time / total_duration) * 100
                        yield progress
                except Exception as e:
                    print(f"解析进度时发生错误: {e}")

        process.wait()
        yield 100

    def get_video_duration(self, path):
        """获取视频总时长（秒）"""
        try:
            cap = cv2.VideoCapture(path)
            if not cap.isOpened():
                return 0
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            fps = cap.get(cv2.CAP_PROP_FPS)
            cap.release()
            return total_frames / fps if fps > 0 else 0
        except Exception:
            return 0

    def get_file_names(self, directory):
        """获取指定文件夹中的所有文件名"""
        try:
            return [file for file in os.listdir(directory) if os.path.isfile(os.path.join(directory, file))]
        except Exception as e:
            print(f"发生错误: {e}")
            return []

    def upload(self, out_path):
        """上传处理后的图片或视频文件到远程服务器"""
        upload_url = "http://localhost:9999/files/upload"
        try:
            with open(out_path, 'rb') as file:
                files = {'file': (os.path.basename(out_path), file)}
                response = requests.post(upload_url, files=files)
                if response.status_code == 200:
                    print("文件上传成功！")
                    return response.json()['data']
                else:
                    print("文件上传失败！")
        except Exception as e:
            print(f"上传文件时发生错误: {str(e)}")

    def download(self, url, save_path):
        """下载文件并保存到指定路径"""
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        try:
            with requests.get(url, stream=True) as response:
                response.raise_for_status()
                with open(save_path, 'wb') as file:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            file.write(chunk)
            print(f"文件已成功下载并保存到 {save_path}")
        except requests.RequestException as e:
            print(f"下载失败: {e}")

    def cleanup_files(self, file_paths):
        """清理文件"""
        for path in file_paths:
            if os.path.exists(path):
                os.remove(path)

    def cleanup_resources(self, cap, video_writer):
        """释放资源"""
        if cap.isOpened():
            cap.release()
        if video_writer is not None:
            video_writer.release()
        cv2.destroyAllWindows()

    def is_remote_resource(self, path):
        """判断输入路径是否为远程资源"""
        if not path:
            return False
        parsed = urlparse(path)
        return parsed.scheme in ('http', 'https')

    def resolve_local_image_path(self, path):
        """解析本地图片路径"""
        if path.startswith("file://"):
            path = path[7:]
        normalized = os.path.normpath(path)
        if not os.path.isabs(normalized):
            normalized = os.path.abspath(os.path.join(self.app.root_path, normalized))
        return normalized

    def open_camera(self):
        """尝试打开外接或内置摄像头，返回成功的 VideoCapture 实例、索引以及调试信息"""
        preferred = os.getenv("CAMERA_INDEX")
        candidates = []
        if preferred:
            try:
                candidates = [int(idx.strip()) for idx in preferred.split(',') if idx.strip()]
            except ValueError:
                print(f"CAMERA_INDEX 环境变量解析失败: {preferred}")
                candidates = []
        if not candidates:
            # 默认优先尝试索引 1（常见为外接摄像头），其后回退到 0、2、3...
            candidates = [1, 0] + [idx for idx in range(2, 8)]

        backend_options = []
        if os.name == 'nt':
            for name in ("CAP_DSHOW", "CAP_MSMF"):
                if hasattr(cv2, name):
                    backend_options.append(getattr(cv2, name))
        backend_options.append(getattr(cv2, "CAP_ANY", 0))

        diagnostics = []

        for idx in candidates:
            for backend in backend_options:
                try:
                    print(f"尝试打开摄像头索引 {idx} 后端 {backend}...")
                    if backend == getattr(cv2, "CAP_ANY", 0):
                        cap = cv2.VideoCapture(idx)
                    else:
                        cap = cv2.VideoCapture(idx, backend)
                except Exception as exc:
                    print(f"打开摄像头索引 {idx} 失败: {exc}")
                    diagnostics.append({
                        'index': idx,
                        'backend': int(backend),
                        'status': 'error',
                        'message': str(exc),
                    })
                    continue

                if cap is None or not cap.isOpened():
                    diagnostics.append({
                        'index': idx,
                        'backend': int(backend),
                        'status': 'fail',
                        'message': '设备未打开',
                    })
                    if cap is not None:
                        cap.release()
                    continue

                frame_ok = False
                for _ in range(5):
                    ret, frame = cap.read()
                    if ret and frame is not None:
                        frame_ok = True
                        break
                    time.sleep(0.1)

                if frame_ok:
                    print(f"摄像头索引 {idx} 打开成功。")
                    diagnostics.append({
                        'index': idx,
                        'backend': int(backend),
                        'status': 'success'
                    })
                    return cap, idx, diagnostics

                print(f"摄像头索引 {idx} 未读取到有效帧，继续尝试其他设备。")
                diagnostics.append({
                    'index': idx,
                    'backend': int(backend),
                    'status': 'fail',
                    'message': '无有效帧'
                })
                cap.release()

        print("未检测到可用摄像头。")
        return None, None, diagnostics

    def get_or_load_model(self, weight_name):
        """获取已加载的模型或加载新模型，并返回加载耗时"""
        if weight_name in self.model_cache and weight_name in self.model_ready:
            return self.model_cache[weight_name], 0.0

        load_start = time.time()
        try:
            self.socketio.emit('model_loading', {
                'weight': weight_name,
                'progress': 0,
                'status': 'loading'
            })
        except Exception:
            pass  # 如果当前没有活跃的Socket客户端，不影响加载过程

        model = YOLO(f'./weights/{weight_name}')
        if self.device != "cpu":
            model.to(self.device)
        # 主动热身以避免首次推理抖动
        try:
            model.warmup(imgsz=(1, 3, 640, 640))
        except Exception:
            dummy_frame = np.zeros((640, 640, 3), dtype=np.uint8)
            model.predict(
                source=dummy_frame,
                imgsz=640,
                device=self.device,
                half=self.use_half,
                conf=0.25,
                verbose=False
            )
        self.model_cache[weight_name] = model
        self.model_ready.add(weight_name)

        load_duration = time.time() - load_start
        try:
            self.socketio.emit('model_loading', {
                'weight': weight_name,
                'progress': 100,
                'status': 'ready',
                'duration': load_duration
            })
        except Exception:
            pass

        return model, load_duration

    def schedule_async_upload(self, result_path, result_filename):
        """异步上传预测结果，避免阻塞响应"""
        def _upload():
            uploaded_url = self.upload(result_path)
            if uploaded_url:
                print(f"异步上传完成: {uploaded_url}")
            else:
                print(f"异步上传失败: {result_filename}")

        thread = threading.Thread(target=_upload, name=f"upload-{result_filename}", daemon=True)
        thread.start()

    def emit_task_event(self, task_id, status, **extra):
        """向前端推送任务进度事件"""
        if not task_id:
            return
        payload = {
            'taskId': task_id,
            'status': status
        }
        payload.update({k: v for k, v in extra.items() if v is not None})
        try:
            self.socketio.emit('task_progress', payload)
        except Exception:
            pass


# 启动应用
if __name__ == '__main__':
    print("=" * 60)
    print("🚀 正在启动YOLO农作物病害检测Flask服务...")
    print("=" * 60)
    
    try:
        print("📦 正在初始化Flask应用...")
        video_app = VideoProcessingApp()
        print("✅ Flask应用初始化成功")
        device_label = "GPU" if video_app.device != "cpu" else "CPU"
        print(f"🧠 推理设备: {device_label} ({video_app.device})")
        if video_app.device == "cpu":
            print("⚠️ 未检测到可用GPU，已自动回退到CPU。")
        
        print("🔍 正在检查模型文件...")
        import os
        weights_dir = "./weights"
        if os.path.exists(weights_dir):
            model_files = [f for f in os.listdir(weights_dir) if f.endswith('.pt')]
            print(f"📁 发现 {len(model_files)} 个模型文件:")
            for model in model_files:
                file_size = os.path.getsize(os.path.join(weights_dir, model)) / (1024*1024)
                print(f"   - {model} ({file_size:.1f} MB)")
        else:
            print("⚠️  警告: weights目录不存在!")
        
        print(f"服务地址: http://localhost:5001")
        print("正在启动Web服务器...")
        print("提示: 首次启动可能需要30-60秒加载模型")
        print("请耐心等待，不要关闭此窗口...")
        print("=" * 60)
        
        video_app.run()
    except Exception as e:
        print("❌ Flask应用启动失败!")
        print(f"错误信息: {str(e)}")
        import traceback
        traceback.print_exc()
        print("=" * 60)
        print("🔧 可能的解决方案:")
        print("1. 检查Python依赖是否正确安装")
        print("2. 检查模型文件是否存在")
        print("3. 检查端口5001是否被占用")
        print("=" * 60)
