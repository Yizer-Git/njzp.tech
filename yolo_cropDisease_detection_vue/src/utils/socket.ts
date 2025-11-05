import { io } from 'socket.io-client';
 
export class SocketService {
  private static instance: SocketService;
  private socket;
  private eventListeners: Map<string, Function[]> = new Map();

  private constructor() {
    const baseUrl = import.meta.env.VITE_FLASK_SOCKET_URL || 'http://localhost:5001';
    this.socket = io(baseUrl);
  }

  // 单例模式：确保整个应用只有一个实例
  public static getInstance(): SocketService {
    if (!SocketService.instance) {
      SocketService.instance = new SocketService();
    }
    return SocketService.instance;
  }
 
  on(event: string, callback: Function) {
    // 清理之前的监听器
    this.off(event);
    
    // 添加新的监听器
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)!.push(callback);
    
    this.socket.on(event, (data) => {
      if (data && Object.prototype.hasOwnProperty.call(data, 'data')) {
        callback(data.data);
      } else {
        callback(data);
      }
    });
  }
  
  off(event: string) {
    if (this.eventListeners.has(event)) {
      this.eventListeners.delete(event);
    }
    this.socket.off(event);
  }
 
  emit(event: string, data: any) {
    this.socket.emit(event, data);
  }
 
  disconnect() {
    this.eventListeners.clear();
    this.socket.disconnect();
  }
}
