<script setup lang="ts">
defineProps({
  contacts: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <div class="contact-list">
    <div 
      v-for="contact in contacts" 
      :key="contact.id"
      class="contact-item"
    >
      <div class="avatar">
        <img src="../assets/logo.svg" alt="avatar" />
        <div class="status" :class="contact.status"></div>
      </div>
      <div class="contact-name">{{ contact.name }}</div>
    </div>
  </div>
</template>

<style scoped>
.contact-list {
  padding: 5px;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 5px;
  margin: 5px;
}

.contact-item:hover {
  background-color: #f0f0f0;
}

.avatar {
  position: relative;
  margin-right: 10px;
}

.avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ddd;
}

.status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border: 2px solid white;
  border-radius: 50%;
}

.status.online {
  background-color: #4caf50;
}

.status.offline {
  background-color: #9e9e9e;
}

.status.busy {
  background-color: #f44336;
}

.contact-name {
  font-size: 14px;
  color: #333;
}
</style>