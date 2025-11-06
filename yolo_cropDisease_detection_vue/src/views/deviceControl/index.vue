<template>
  <div class="app-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>硬件远程控制台</span>
          <el-button type="primary" link @click="loadDevices">刷新设备</el-button>
        </div>
      </template>

      <el-row :gutter="20" class="control-row">
        <el-col :xs="24" :sm="12" :md="8">
          <div class="device-selector">
            <span class="device-selector__label">目标设备</span>
            <el-select v-model="selectedDeviceId" placeholder="请选择设备" clearable>
              <el-option
                v-for="device in deviceOptions"
                :key="device"
                :label="device"
                :value="device"
              />
            </el-select>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="never" class="control-card">
            <div class="control-card__header">水泵 (继电器1)</div>
            <div class="control-card__body">
              <el-switch
                v-model="relayWaterState"
                :disabled="!selectedDeviceId || loading"
                active-text="开启"
                inactive-text="关闭"
                @change="handleRelayChange('RELAY_WATER', $event)"
              />
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="never" class="control-card">
            <div class="control-card__header">风扇 (继电器2)</div>
            <div class="control-card__body">
              <el-switch
                v-model="relayFanState"
                :disabled="!selectedDeviceId || loading"
                active-text="开启"
                inactive-text="关闭"
                @change="handleRelayChange('RELAY_FAN', $event)"
              />
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="never" class="control-card">
            <div class="control-card__header">警报器 (蜂鸣器)</div>
            <div class="control-card__body">
              <el-button
                type="danger"
                :loading="loading && activeAction === 'BUZZER'"
                :disabled="!selectedDeviceId"
                @click="triggerBuzzer"
              >
                触发蜂鸣器 (1秒)
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-alert
        v-if="!deviceOptions.length"
        type="warning"
        show-icon
        title="暂无设备数据，请先确认传感器终端已上报数据"
        class="empty-alert"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { controlDevice } from '/@/api/device';
import { fetchSensorDevices } from '/@/api/sensor';

const deviceOptions = ref<string[]>([]);
const selectedDeviceId = ref<string>('');
const relayWaterState = ref(false);
const relayFanState = ref(false);
const loading = ref(false);
const activeAction = ref('');

const loadDevices = async () => {
  try {
    const res = await fetchSensorDevices();
    if (res.code === 0 && Array.isArray(res.data)) {
      deviceOptions.value = res.data;
      if (!selectedDeviceId.value && deviceOptions.value.length) {
        selectedDeviceId.value = deviceOptions.value[0];
      }
    } else {
      ElMessage.warning('未获取到可用设备');
    }
  } catch (error) {
    console.error('获取设备列表失败', error);
    ElMessage.error('获取设备列表失败');
  }
};

const sendCommand = async (action: string, value: any) => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请先选择设备');
    return false;
  }

  try {
    loading.value = true;
    activeAction.value = action;
    const res = await controlDevice({
      deviceId: selectedDeviceId.value,
      action,
      value
    });

    if (res.code === 0) {
      ElMessage.success('指令已发送');
      return true;
    }

    throw new Error(res.msg || '发送失败');
  } catch (error) {
    console.error('发送控制指令失败', error);
    ElMessage.error(error instanceof Error ? error.message : '发送指令失败');
    return false;
  } finally {
    loading.value = false;
    activeAction.value = '';
  }
};

const handleRelayChange = async (action: string, value: boolean) => {
  const targetRef = action === 'RELAY_WATER' ? relayWaterState : relayFanState;
  const succeed = await sendCommand(action, value);
  if (!succeed) {
    targetRef.value = !value;
  }
};

const triggerBuzzer = async () => {
  const succeed = await sendCommand('BUZZER', true);
  if (!succeed) {
    return;
  }
};

onMounted(async () => {
  await loadDevices();
});
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
}

.control-row {
  margin-bottom: 10px;
}

.device-selector {
  display: flex;
  flex-direction: column;

  &__label {
    font-size: 14px;
    color: #666;
    margin-bottom: 6px;
  }
}

.control-card {
  margin-bottom: 20px;

  &__header {
    font-weight: 600;
    margin-bottom: 10px;
  }

  &__body {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 120px;
  }
}

.empty-alert {
  margin-top: 20px;
}
</style>
