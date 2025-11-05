<template>
  <div class="sensor-container">
    <el-card class="header-card">
      <h2>环境数据监测</h2>
      <p class="subtitle">实时监测温度、湿度、土壤墒情、光照强度、CO2浓度</p>
    </el-card>

    <!-- 实时数据卡片 -->
    <el-row :gutter="20" class="data-cards">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card temperature-card" shadow="hover">
          <div class="card-icon">
            <i class="el-icon-sunny"></i>
          </div>
          <div class="card-content">
            <div class="card-title">温度</div>
            <div class="card-value">{{ latestData.temperature || '--' }}℃</div>
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
            <div class="card-title">湿度</div>
            <div class="card-value">{{ latestData.humidity || '--' }}%</div>
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
            <div class="card-title">土壤墒情</div>
            <div class="card-value">{{ latestData.soilMoisture || '--' }}%</div>
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
            <div class="card-value">{{ formatLight(latestData.lightIntensity) }}</div>
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
            <div class="card-title">CO2浓度</div>
            <div class="card-value">{{ latestData.co2Level || '--' }} ppm</div>
            <div class="card-time">{{ formatTime(latestData.timestamp) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card class="data-card refresh-card" shadow="hover" @click.native="refreshData">
          <div class="card-icon refresh-icon">
            <i class="el-icon-refresh" :class="{ rotating: loading }"></i>
          </div>
          <div class="card-content">
            <div class="card-title">刷新数据</div>
            <div class="card-value" style="font-size: 14px">点击刷新</div>
            <div class="card-time">{{ autoRefreshText }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史趋势图表 -->
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
import request from '@/utils/request';

export default {
  name: 'SensorMonitor',
  data() {
    return {
      latestData: {},
      historyData: [],
      loading: false,
      timeRange: '7',
      autoRefresh: null,
      autoRefreshText: '自动刷新：30秒',
      chart: null
    };
  },
  mounted() {
    this.loadLatestData();
    this.loadHistoryData();
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
    /**
     * 加载最新数据
     */
    async loadLatestData() {
      try {
        this.loading = true;
        const response = await request({
          url: '/api/sensor/latest',
          method: 'get'
        });
        
        if (response.code === '200' && response.data) {
          this.latestData = response.data;
        }
      } catch (error) {
        console.error('加载最新传感器数据失败', error);
        this.$message.error('加载数据失败');
      } finally {
        this.loading = false;
      }
    },

    /**
     * 加载历史数据
     */
    async loadHistoryData() {
      try {
        const endTime = new Date();
        const startTime = new Date(endTime.getTime() - this.timeRange * 24 * 60 * 60 * 1000);
        
        const response = await request({
          url: '/api/sensor/history',
          method: 'get',
          params: {
            startTime: this.formatDateTime(startTime),
            endTime: this.formatDateTime(endTime)
          }
        });
        
        if (response.code === '200' && response.data) {
          this.historyData = response.data;
          this.updateChart();
        }
      } catch (error) {
        console.error('加载历史传感器数据失败', error);
      }
    },

    /**
     * 初始化图表
     */
    initChart() {
      const chartDom = document.getElementById('sensorChart');
      this.chart = echarts.init(chartDom);
      this.updateChart();
    },

    /**
     * 更新图表
     */
    updateChart() {
      if (!this.chart || !this.historyData.length) {
        return;
      }

      // 准备数据
      const timestamps = this.historyData.map(item => this.formatTime(item.timestamp));
      const temperatures = this.historyData.map(item => item.temperature);
      const humidities = this.historyData.map(item => item.humidity);
      const soilMoistures = this.historyData.map(item => item.soilMoisture);
      const lightIntensities = this.historyData.map(item => (item.lightIntensity / 1000).toFixed(1));
      const co2Levels = this.historyData.map(item => item.co2Level);

      const option = {
        title: {
          text: '环境数据历史趋势',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['温度(℃)', '湿度(%)', '土壤墒情(%)', '光照强度(klux)', 'CO2(ppm)'],
          top: 30
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: timestamps
        },
        yAxis: [
          {
            type: 'value',
            name: '温度/湿度/土壤',
            position: 'left'
          },
          {
            type: 'value',
            name: '光照/CO2',
            position: 'right'
          }
        ],
        series: [
          {
            name: '温度(℃)',
            type: 'line',
            data: temperatures,
            smooth: true,
            itemStyle: { color: '#FF6B6B' }
          },
          {
            name: '湿度(%)',
            type: 'line',
            data: humidities,
            smooth: true,
            itemStyle: { color: '#4ECDC4' }
          },
          {
            name: '土壤墒情(%)',
            type: 'line',
            data: soilMoistures,
            smooth: true,
            itemStyle: { color: '#95E1D3' }
          },
          {
            name: '光照强度(klux)',
            type: 'line',
            data: lightIntensities,
            smooth: true,
            yAxisIndex: 1,
            itemStyle: { color: '#FFD93D' }
          },
          {
            name: 'CO2(ppm)',
            type: 'line',
            data: co2Levels,
            smooth: true,
            yAxisIndex: 1,
            itemStyle: { color: '#A8E6CF' }
          }
        ]
      };

      this.chart.setOption(option);
    },

    /**
     * 刷新数据
     */
    refreshData() {
      this.loadLatestData();
      this.loadHistoryData();
    },

    /**
     * 开始自动刷新
     */
    startAutoRefresh() {
      this.autoRefresh = setInterval(() => {
        this.loadLatestData();
      }, 30000); // 每30秒刷新一次
    },

    /**
     * 停止自动刷新
     */
    stopAutoRefresh() {
      if (this.autoRefresh) {
        clearInterval(this.autoRefresh);
        this.autoRefresh = null;
      }
    },

    /**
     * 格式化时间
     */
    formatTime(timestamp) {
      if (!timestamp) return '--';
      const date = new Date(timestamp);
      return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    },

    /**
     * 格式化日期时间（用于API请求）
     */
    formatDateTime(date) {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      const seconds = date.getSeconds().toString().padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    },

    /**
     * 格式化光照强度
     */
    formatLight(value) {
      if (!value) return '--';
      if (value >= 1000) {
        return (value / 1000).toFixed(1) + ' klux';
      }
      return value + ' lux';
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
  text-align: center;

  h2 {
    margin: 0;
    color: #333;
  }

  .subtitle {
    margin: 10px 0 0 0;
    color: #999;
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
      color: #ccc;
    }
  }
}

.temperature-card .card-icon {
  color: #FF6B6B;
}

.humidity-card .card-icon {
  color: #4ECDC4;
}

.soil-card .card-icon {
  color: #95E1D3;
}

.light-card .card-icon {
  color: #FFD93D;
}

.co2-card .card-icon {
  color: #A8E6CF;
}

.refresh-card .card-icon {
  color: #6C5CE7;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.chart-card {
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      color: #333;
    }
  }
}
</style>


