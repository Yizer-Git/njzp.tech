import { io } from 'socket.io-client';
 
export class SocketService {
  private socket;

  constructor() {
    const baseUrl = import.meta.env.VITE_FLASK_SOCKET_URL || 'http://localhost:5001';
    this.socket = io(baseUrl);
  }
 
  on(event: string, callback: Function) {
    this.socket.on(event, (data) => {
      if (data && Object.prototype.hasOwnProperty.call(data, 'data')) {
        callback(data.data);
      } else {
        callback(data);
      }
    });
  }
 
  emit(event: string, data: any) {
    this.socket.emit(event, data);
  }
 
  disconnect() {
    this.socket.disconnect();
  }
}
