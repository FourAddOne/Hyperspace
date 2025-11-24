// API端点常量定义

export const API_ENDPOINTS = {
  // 用户认证相关
  USER_LOGIN: '/user/login',
  USER_REGISTER: '/user/register',
  USER_LOGOUT: '/user/logout',
  
  // 用户信息相关
  USER_PROFILE: '/user/profile',
  USER_UPDATE: '/user/update',
  
  // 用户设置相关
  USER_SETTINGS: '/user/settings',
  
  // 聊天相关
  CHAT_MESSAGES: '/chat/messages',
  CHAT_SEND: '/chat/send',
  CHAT_HISTORY: '/chat/history',
  
  // 联系人相关
  CONTACTS_LIST: '/contacts',
  CONTACTS_ADD: '/contacts/add',
  CONTACTS_REMOVE: '/contacts/remove',
  
  // 群组相关
  CREATEGROUP: '/api/groups/create',
  JOINGROUP: '/api/groups/join',
  QUITGROUP: '/api/groups/quit',
  GROUPLIST: '/api/groups/list',
  GROUPMEMBERS: '/api/groups/members',
  GROUPMESSAGE: '/api/groups/message'
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