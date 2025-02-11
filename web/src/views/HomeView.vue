<template>
  <div class="container mx-auto px-4 py-8">
    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- Total Products Card -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">Total Products</p>
            <p class="text-2xl font-bold">{{ totalProducts }}</p>
          </div>
          <div class="bg-blue-100 p-3 rounded-full">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Total Value Card -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">Total Value</p>
            <p class="text-2xl font-bold">${{ formatNumber(totalValue) }}</p>
          </div>
          <div class="bg-green-100 p-3 rounded-full">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Average Price Card -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">Average Price</p>
            <p class="text-2xl font-bold">${{ formatNumber(averagePrice) }}</p>
          </div>
          <div class="bg-purple-100 p-3 rounded-full">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Total Quantity Card -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm">Total Quantity</p>
            <p class="text-2xl font-bold">{{ formatNumber(totalQuantity) }}</p>
          </div>
          <div class="bg-yellow-100 p-3 rounded-full">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Region Distribution Chart -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <h3 class="text-lg font-semibold mb-4">Regional Distribution</h3>
        <div class="h-64">
          <canvas ref="regionChart"></canvas>
        </div>
      </div>

      <!-- Time Series Chart -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <h3 class="text-lg font-semibold mb-4">Monthly Trends</h3>
        <div class="h-64">
          <canvas ref="timeChart"></canvas>
        </div>
      </div>
      <footer>
        <p class="text-center text-gray-500 text-sm">&copy; 2025 Product Manager</p>
      </footer>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import Chart from 'chart.js/auto'

export default {
  name: 'Home',
  setup() {
    const rawData = ref(null)
    const regionChart = ref(null)
    const timeChart = ref(null)

    const statistics = computed(() => rawData.value?.data?.[0] || {})
    const totalProducts = computed(() => statistics.value?.totalProducts?.[0]?.total || 0)
    const totalValue = computed(() => statistics.value?.globalStats?.[0]?.totalValue || 0)
    const averagePrice = computed(() => statistics.value?.globalStats?.[0]?.averagePrice || 0)
    const totalQuantity = computed(() => statistics.value?.globalStats?.[0]?.totalQuantity || 0)

    const fetchStatistics = async () => {
      try {
        const response = await fetch('http://localhost:8888/private/product/statistics', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        const data = await response.json()
        rawData.value = data
        initCharts()
      } catch (error) {
        console.error('Error fetching statistics:', error)
      }
    }

    const initCharts = () => {
      if (regionChart.value) {
        const existingChart = Chart.getChart(regionChart.value)
        if (existingChart) existingChart.destroy()
      }
      if (timeChart.value) {
        const existingChart = Chart.getChart(timeChart.value)
        if (existingChart) existingChart.destroy()
      }

      if (statistics.value.regionStats) {
        new Chart(regionChart.value, {
          type: 'doughnut',
          data: {
            labels: statistics.value.regionStats.map(item => item._id),
            datasets: [{
              data: statistics.value.regionStats.map(item => item.count),
              backgroundColor: [
                '#3B82F6',
                '#10B981',
                '#F59E0B',
                '#EF4444',
                '#8B5CF6',
                '#EC4899',
                '#6366F1',
                '#14B8A6'
              ]
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'right'
              }
            }
          }
        })
      }

      // Time Chart
      if (statistics.value.timeStats) {
        new Chart(timeChart.value, {
          type: 'line',
          data: {
            labels: statistics.value.timeStats.map(item =>
              `${item._id.year}-${String(item._id.month).padStart(2, '0')}`
            ),
            datasets: [{
              label: 'Products',
              data: statistics.value.timeStats.map(item => item.count),
              borderColor: '#3B82F6',
              backgroundColor: 'rgba(59, 130, 246, 0.1)',
              tension: 0.4,
              fill: true
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                display: false
              }
            },
            scales: {
              y: {
                beginAtZero: true
              }
            }
          }
        })
      }
    }

    const formatNumber = (number) => {
      return new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(number)
    }

    onMounted(() => {
      fetchStatistics()
    })

    return {
      statistics,
      totalProducts,
      totalValue,
      averagePrice,
      totalQuantity,
      regionChart,
      timeChart,
      formatNumber
    }
  }
}
</script>
