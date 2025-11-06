<template>
  <div class="sensor-container">
    <el-card class="header-card">
      <div class="header-top">
        <div>
          <h2>环境数据监测</h2>
          <p class="subtitle">实时跟踪空气温湿度、土壤湿度、光照强度、水位状态与 CO₂ 浓度</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="refreshData">
            手动刷新
          </el-button>
          <span class="auto-refresh">{{ autoRefreshText }}</span>
          <el-select
            v-model="selectedDeviceId"
            placeholder="请选择设备"
            clearable
            class="device-select"
            @change="handleDeviceChange"
          >
            <el-option
              v-for="device in deviceOptions"
              :key="device"
              :label="device"
              :value="device"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="data-cards">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card temperature-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-sunny"></i>
          </div>
          <div class="card-content">
            <div class="card-title">空气温度</div>
            <div class="card-value">{{ displayValue(getField(latestData, ['airTemperature', 'temperature']), '℃') }}</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card humidity-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-water-cup"></i>
          </div>
          <div class="card-content">
            <div class="card-title">空气湿度</div>
            <div class="card-value">{{ displayValue(getField(latestData, ['airHumidity', 'humidity']), '%') }}</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card soil-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-cloudy"></i>
          </div>
          <div class="card-content">
            <div class="card-title">土壤湿度</div>
            <div class="card-value">{{ displayValue(getField(latestData, ['soilHumidity', 'soilMoisture']), '%') }}</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card light-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-light"></i>
          </div>
          <div class="card-content">
            <div class="card-title">光照强度</div>
            <div class="card-value">{{ formatLight(getField(latestData, ['lightIntensity'])) }}</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card water-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-refrigerator"></i>
          </div>
          <div class="card-content">
            <div class="card-title">水位状态</div>
            <div class="card-value">{{ formatWaterLevel(getField(latestData, ['waterLevel'])) }}</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card co2-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-wind-power"></i>
          </div>
          <div class="card-content">
            <div class="card-title">CO₂ 浓度</div>
            <div class="card-value">{{ displayValue(getField(latestData, ['co2Level']), ' ppm') }}</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <div class="table-header">
        <h3>最近数据明细</h3>
      </div>
      <el-table :data="historyData" size="small" border stripe :height="tableHeight">
        <el-table-column prop="timestamp" label="采集时间" align="center" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="airTemperature" label="空气温度(℃)" align="center">
          <template #default="scope">
            {{ displayValue(getField(scope.row, ['airTemperature', 'temperature']), '') }}
          </template>
        </el-table-column>
        <el-table-column prop="airHumidity" label="空气湿度(%)" align="center">
          <template #default="scope">
            {{ displayValue(getField(scope.row, ['airHumidity', 'humidity']), '') }}
          </template>
        </el-table-column>
        <el-table-column prop="soilHumidity" label="土壤湿度(%)" align="center">
          <template #default="scope">
            {{ displayValue(getField(scope.row, ['soilHumidity', 'soilMoisture']), '') }}
          </template>
        </el-table-column>
        <el-table-column prop="lightIntensity" label="光照强度(lux)" align="center">
          <template #default="scope">
            {{ getField(scope.row, ['lightIntensity']) ?? '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="waterLevel" label="水位状态" align="center">
          <template #default="scope">
            {{ formatWaterLevel(getField(scope.row, ['waterLevel'])) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="chart-card">
      <div class="chart-header">
        <h3>历史数据趋势</h3>
        <div class="chart-controls">
          <el-radio-group v-model="timeRange" size="small" @change="loadHistoryData">
            <el-radio-button label="1">近1天</el-radio-button>
            <el-radio-button label="3">近3天</el-radio-button>
            <el-radio-button label="7">近7天</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div id="sensorChart" style="width: 100%; height: 400px"></div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { Refresh } from '@element-plus/icons-vue';
import { fetchLatestSensorData, fetchSensorDevices, fetchSensorHistory } from '/@/api/sensor';

export default {
  name: 'SensorMonitor',
  components: { Refresh },
  data() {
    return {
      deviceOptions: [],
      selectedDeviceId: '',
      latestData: {},
      historyData: [],
      loading: false,
      timeRange: '7',
      autoRefresh: null,
      autoRefreshText: '自动刷新：30 秒',
      chart: null,
      mockHistoryCache: [],
      tableHeight: 420
    };
  },
  async mounted() {
    await this.loadDeviceOptions();
    await this.loadLatestData();
    await this.loadHistoryData();
    this.initChart();
    this.startAutoRefresh();
  },
  beforeDestroy() {
    this.stopAutoRefresh();
    if (this.chart) {
      this.chart.dispose();
    }
  },
  methods: {
    async loadDeviceOptions() {
      try {
        const res = await fetchSensorDevices();
        if (res.code === 0 && Array.isArray(res.data)) {
          this.deviceOptions = res.data;
          if (!this.selectedDeviceId && this.deviceOptions.length) {
            this.selectedDeviceId = this.deviceOptions[0];
          }
        }
      } catch (error) {
        console.error('加载设备列表失败', error);
        this.$message.error('无法获取设备列表');
      }
    },

    async loadLatestData() {
      if (!this.selectedDeviceId) {
        this.latestData = {};
        return;
      }
      try {
        this.loading = true;
        const response = await fetchLatestSensorData(this.selectedDeviceId);
        if (response.code === 0 && response.data) {
          this.latestData = response.data;
        } else {
          const fallback = this.getMockHistory();
          this.latestData = fallback[fallback.length - 1] || {};
        }
      } catch (error) {
        console.error('加载最新传感器数据失败', error);
        this.$message.error('加载数据失败');
        const fallback = this.getMockHistory();
        this.latestData = fallback[fallback.length - 1] || {};
      } finally {
        this.loading = false;
      }
    },

    async loadHistoryData() {
      if (!this.selectedDeviceId) {
        this.historyData = [];
        return;
      }
      try {
        const endTime = new Date();
        const startTime = new Date(endTime.getTime() - Number(this.timeRange) * 24 * 60 * 60 * 1000);

        const response = await fetchSensorHistory({
          deviceId: this.selectedDeviceId,
          startTime: this.formatDateTime(startTime),
          endTime: this.formatDateTime(endTime)
        });

        if (response.code === 0 && Array.isArray(response.data) && response.data.length) {
          this.historyData = response.data;
        } else {
          this.historyData = this.getMockHistory();
        }
        this.updateChart();
      } catch (error) {
        console.error('加载历史传感器数据失败', error);
        this.historyData = this.getMockHistory();
        this.updateChart();
      }
    },

    initChart() {
      const chartDom = document.getElementById('sensorChart');
      this.chart = echarts.init(chartDom);
      this.updateChart();
    },

    updateChart() {
      if (!this.chart || !this.historyData.length) {
        this.chart && this.chart.clear();
        return;
      }

      const timestamps = this.historyData.map((item) => this.formatTime(item.timestamp));
      const airTemps = this.historyData.map((item) => this.getField(item, ['airTemperature', 'temperature']));
      const airHumidity = this.historyData.map((item) => this.getField(item, ['airHumidity', 'humidity']));
      const soilHumidity = this.historyData.map((item) => this.getField(item, ['soilHumidity', 'soilMoisture']));
      const lightIntensity = this.historyData.map((item) => {
        const val = this.getField(item, ['lightIntensity']);
        return val ? (val / 1000).toFixed(1) : null;
      });
      const co2Levels = this.historyData.map((item) => this.getField(item, ['co2Level']));

      const option = {
        title: {
          text: '环境数据历史趋势',
          left: 'center'
        },
        tooltip: { trigger: 'axis' },
        legend: {
          data: ['空气温度(℃)', '空气湿度(%)', '土壤湿度(%)', '光照强度(klux)', 'CO₂(ppm)'],
          top: 30
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: timestamps },
        yAxis: [
          { type: 'value', name: '温度/湿度/土壤', position: 'left' },
          { type: 'value', name: '光照/CO₂', position: 'right' }
        ],
        series: [
          { name: '空气温度(℃)', type: 'line', data: airTemps, smooth: true, itemStyle: { color: '#FF6B6B' } },
          { name: '空气湿度(%)', type: 'line', data: airHumidity, smooth: true, itemStyle: { color: '#4ECDC4' } },
          { name: '土壤湿度(%)', type: 'line', data: soilHumidity, smooth: true, itemStyle: { color: '#95E1D3' } },
          { name: '光照强度(klux)', type: 'line', data: lightIntensity, smooth: true, yAxisIndex: 1, itemStyle: { color: '#FFD93D' } },
          { name: 'CO₂(ppm)', type: 'line', data: co2Levels, smooth: true, yAxisIndex: 1, itemStyle: { color: '#A8E6CF' } }
        ]
      };

      this.chart.setOption(option);
    },

    refreshData() {
      this.loadLatestData();
      this.loadHistoryData();
    },

    startAutoRefresh() {
      this.autoRefresh = setInterval(() => {
        this.loadLatestData();
      }, 30000);
    },

    stopAutoRefresh() {
      if (this.autoRefresh) {
        clearInterval(this.autoRefresh);
        this.autoRefresh = null;
      }
    },

    handleDeviceChange() {
      this.mockHistoryCache = [];
      this.loadLatestData();
      this.loadHistoryData();
    },

    getField(record, keys, defaultValue = null) {
      if (!record) return defaultValue;
      for (const key of keys) {
        if (record[key] !== undefined && record[key] !== null) {
          return record[key];
        }
      }
      return defaultValue;
    },

    displayValue(value, suffix = '') {
      if (value === undefined || value === null || value === '') {
        return '--';
      }
      const num = Number(value);
      if (Number.isNaN(num)) {
        return `${value}${suffix}`;
      }
      return `${num.toFixed(1).replace(/\.0$/, '')}${suffix}`;
    },

    getMockHistory() {
      if (!this.mockHistoryCache.length) {
        this.mockHistoryCache = this.buildFallbackHistory();
      }
      return this.mockHistoryCache;
    },

    buildFallbackHistory() {
      const deviceId = this.selectedDeviceId || 'DEVICE_001';
      const now = new Date();
      const points = [];
      for (let i = 23; i >= 0; i--) {
        const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000);
        const dayProgress = ((24 - i) / 24) * Math.PI;
        const baseTemp = 23 + Math.sin(dayProgress) * 4;
        const baseHumidity = 70 - Math.sin(dayProgress) * 10;
        const baseSoil = 52 - Math.cos(dayProgress) * 4;
        const light = timestamp.getHours() >= 6 && timestamp.getHours() <= 18
          ? Math.max(120, 320 * Math.sin((timestamp.getHours() - 6) / 12 * Math.PI)) + Math.random() * 80
          : Math.random() * 15;
        const waterLevel = baseSoil < 42 ? 1 : baseSoil > 60 ? 2 : 0;

        points.push({
          deviceId,
          temperature: Number((baseTemp + (Math.random() - 0.5) * 1.2).toFixed(1)),
          humidity: Number((baseHumidity + (Math.random() - 0.5) * 4).toFixed(1)),
          soilMoisture: Number((baseSoil + (Math.random() - 0.5) * 3).toFixed(1)),
          lightIntensity: Math.round(light),
          waterLevel,
          co2Level: Number((410 + Math.cos(dayProgress) * 12 + (Math.random() - 0.5) * 10).toFixed(1)),
          timestamp: timestamp.toISOString()
        });
      }
      return points;
    },

    formatTime(timestamp) {
      if (!timestamp) return '--';
      const date = new Date(timestamp);
      if (Number.isNaN(date.getTime())) {
        return timestamp;
      }
      return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date
        .getMinutes()
        .toString()
        .padStart(2, '0')}`;
    },

    formatDateTime(date) {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      const seconds = date.getSeconds().toString().padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    },

    formatLight(value) {
      if (value === undefined || value === null || value === '') {
        return '--';
      }
      const numeric = Number(value);
      if (Number.isNaN(numeric)) {
        return value;
      }
      if (numeric >= 1000) {
        return `${(numeric / 1000).toFixed(1)} klux`;
      }
      return `${numeric} lux`;
    },

    formatWaterLevel(value) {
      const levelMap = {
        0: '正常',
        1: '缺水',
        2: '水满'
      };
      if (value === undefined || value === null) {
        return '--';
      }
      return levelMap[value] || `未知(${value})`;
    }
  }
};
</script>

<style scoped lang="scss">
.sensor-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
  .header-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;

    h2 {
      margin: 0;
      color: #333;
    }

    .subtitle {
      margin: 10px 0 0 0;
      color: #999;
    }

    .device-select {
      min-width: 220px;
      margin-top: 0;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    justify-content: flex-end;
    margin-left: auto;
  }

  .auto-refresh {
    font-size: 12px;
    color: #777;
  }
}

.data-cards {
  margin-bottom: 20px;
}

.data-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }

  .card-icon {
    font-size: 40px;
    text-align: center;
    margin-bottom: 10px;

    &.refresh-icon {
      cursor: pointer;
    }
  }

  .card-content {
    text-align: center;

    .card-title {
      font-size: 14px;
      color: #999;
      margin-bottom: 5px;
    }

    .card-value {
      font-size: 24px;
      font-weight: bold;
      color: #333;
      margin-bottom: 5px;
    }

    .card-time {
      font-size: 12px;
      color: #aaa;
    }
  }
}

.table-card {
  margin-bottom: 20px;

  .table-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }
}

.chart-card {
  margin-top: 20px;
}

.rotating {
  animation: rotating 1s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
