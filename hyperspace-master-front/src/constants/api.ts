// API端点常量定义

export const API_ENDPOINTS = {
  // 用户认证相关
  USER_LOGIN: '/user/login',
  USER_REGISTER: '/user/register',
  USER_LOGOUT: '/user/logout',
  USER_REFRESH: '/user/refresh',
  
  // 用户信息相关
  USER_PROFILE: '/user/profile',
  USER_UPDATE: '/user/update',
  USER_INFO: '/user/info',
  
  // 用户设置相关
  USER_SETTINGS: '/user/settings',
  
  // 文件上传相关
  FILE_UPLOAD: '/file/upload',
  FILE_DELETE: '/file/delete',
  USER_AVATAR: '/user/avatar',
  
  // OSS相关
  OSS_POLICY: '/oss/policy',
  
  // 聊天相关
  CHAT_MESSAGES: '/chat/messages',
  CHAT_SEND: '/chat/send',
  CHAT_HISTORY: '/chat/history',
  MESSAGE_HISTORY: '/messages/history',
  MESSAGE_UNREAD_COUNTS: '/messages/unread-counts',
  MESSAGE_MARK_AS_READ: '/messages/mark-as-read',
  
  // 联系人相关
  CONTACTS_LIST: '/contacts',
  CONTACTS_ADD: '/contacts/add',
  CONTACTS_REMOVE: '/contacts/remove',
  
  // 好友相关
  FRIENDS_LIST: '/friends/list',
  FRIEND_SEARCH: '/user/search',
  FRIEND_REQUEST: '/friends/request',
  FRIEND_SENT_REQUESTS: '/friends/sent-requests',
  FRIEND_REQUESTS: '/friends/requests',
  FRIEND_ACCEPT: '/friends/accept',
  FRIEND_REJECT: '/friends/reject',
  FRIEND_BLOCK: '/friends/block',
  FRIEND_DELETE: '/friends/delete',
  FRIEND_REMARK: '/friends/remark',
  
  // 群组相关
  CREATEGROUP: '/groups/create',
  JOINGROUP: '/groups/join',
  QUITGROUP: '/groups/quit',
  GROUPLIST: '/groups/list',
  GROUPMEMBERS: '/groups/members',
  GROUPMESSAGE: '/groups/message'
} as const;

// HTTP方法常量
export const HTTP_METHODS = {
  GET: 'GET',
  POST: 'POST',
  PUT: 'PUT',
  DELETE: 'DELETE',
  PATCH: 'PATCH'
} as const;

export default API_ENDPOINTS;