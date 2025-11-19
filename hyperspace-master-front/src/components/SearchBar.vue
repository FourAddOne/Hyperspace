<script setup lang="ts">
import { ref } from 'vue'

const searchQuery = ref('')
</script>

<template>
  <div class="search-bar">
    <input 
      v-model="searchQuery" 
      type="text" 
      placeholder="搜索" 
      class="search-input"
    />
    <span class="search-icon">🔍</span>
  </div>
</template>

<style scoped>
.search-bar {
  padding: 10px 15px;
  position: relative;
  background-color: #ffffff;
}

.search-input {
  width: 100%;
  height: 30px;
  padding: 0 30px 0 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  outline: none;
  background-color: #ffffff;
  color: #1a1a1a;
}

.search-input:focus {
  border-color: #0084ff;
  background-color: #ffffff;
}

.search-input::placeholder {
  color: #999;
}

.search-icon {
  position: absolute;
  right: 25px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #333;
}
</style>