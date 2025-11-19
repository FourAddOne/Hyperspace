import webSocketService from './websocket';

class GlobalWebSocketManager {
  private static instance: GlobalWebSocketManager;
  private connectionCount: number = 0;
  private userStatusCallback: ((data: any) => void) | null = null;
  private messageCallback: ((data: any) => void) | null = null;

  private constructor() {}

  static getInstance(): GlobalWebSocketManager {
    if (!GlobalWebSocketManager.instance) {
      GlobalWebSocketManager.instance = new GlobalWebSocketManager();
    }
    return GlobalWebSocketManager.instance;
  }

  // 增加连接引用计数
  incrementConnection(): void {
    this.connectionCount++;
    console.log('增加WebSocket连接引用计数:', this.connectionCount);
    
    // 每次都尝试建立连接以确保回调是最新的
    console.log('建立WebSocket连接');
    webSocketService.connect(
      this.userStatusCallback || (() => {}), 
      this.messageCallback
    );
  }

  // 减少连接引用计数
  decrementConnection(): void {
    this.connectionCount--;
    console.log('减少WebSocket连接引用计数:', this.connectionCount);
    
    if (this.connectionCount <= 0) {
      // 最后一个引用，断开连接
      console.log('断开WebSocket连接');
      this.connectionCount = 0;
      webSocketService.disconnect();
    }
  }

  // 设置用户状态回调
  setUserStatusCallback(callback: (data: any) => void): void {
    this.userStatusCallback = callback;
  }

  // 设置消息回调
  setMessageCallback(callback: (data: any) => void): void {
    this.messageCallback = callback;
  }

  // 获取连接状态
  isConnected(): boolean {
    return webSocketService.isConnected();
  }

  // 发送用户状态
  sendUserStatus(status: boolean): void {
    webSocketService.sendUserStatus(status);
  }

  // 发送消息
  sendMessage(message: any): void {
    webSocketService.sendMessage(message);
  }
}

export default GlobalWebSocketManager.getInstance();