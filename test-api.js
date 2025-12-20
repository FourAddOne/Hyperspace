const axios = require('axios');

// 测试AI聊天接口
async function testAIChat() {
  try {
    // 不发送token的请求
    console.log('测试不带token的请求...');
    const response1 = await axios.post('http://localhost:8080/api/chatbot/send', {
      message: '你好',
      model: 'deepseek-r1:8b'
    });
    console.log('成功响应:', response1.data);
  } catch (error) {
    console.error('请求失败:', error.response?.status, error.response?.data);
  }

  try {
    // 发送无效token的请求
    console.log('\n测试带无效token的请求...');
    const response2 = await axios.post('http://localhost:8080/api/chatbot/send', {
      message: '你好',
      model: 'deepseek-r1:8b'
    }, {
      headers: {
        'Authorization': 'Bearer invalid-token'
      }
    });
    console.log('成功响应:', response2.data);
  } catch (error) {
    console.error('请求失败:', error.response?.status, error.response?.data);
  }
}

testAIChat();