<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header Section -->
      <div class="mb-8 flex justify-between items-center">
  <h1 class="text-2xl font-bold text-gray-900">Products Management</h1>
  <div class="flex items-center space-x-4">
    <button @click="confirmRemoveAll"
            class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md flex items-center gap-2 transition-colors">
      Remove All
    </button>
    <router-link to="/products/add"
      class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md flex items-center gap-2 transition-colors">
      Add Product
    </router-link>
  </div>
</div>

      <!-- Filters Section -->
      <div class="bg-white p-6 rounded-lg shadow-sm mb-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- Search -->
          <div class="relative">
            <input v-model="filters.search" type="text" placeholder="Search products..."
              class="w-full pl-10 pr-4 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              @input="handleSearch" />
          </div>

          <!-- Category Filter -->
          <select v-model="filters.category"
            class="w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            @change="fetchProducts">
            <option value="">All Categories</option>
            <option v-for="category in categories" :key="category" :value="category">
              {{ category }}
            </option>
          </select>

          <!-- Region Filter -->
          <select v-model="filters.region"
            class="w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            @change="fetchProducts">
            <option value="">All Regions</option>
            <option v-for="region in regions" :key="region" :value="region">
              {{ region }}
            </option>
          </select>
        </div>
      </div>

      <!-- Products Table -->
      <div class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th v-for="header in tableHeaders" :key="header.key"
                class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                <div class="flex items-center gap-1 cursor-pointer" @click="sortBy(header.key)">
                  {{ header.label }}
                  <span class="material-icons-outlined text-sm" v-if="sortColumn === header.key">
                    {{ sortDirection === 'asc' ? 'arrow_upward' : 'arrow_downward' }}
                  </span>
                </div>
              </th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="product in products" :key="product._id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">{{ product.name }}</td>
              <td class="px-6 py-4 whitespace-nowrap">{{ product.category }}</td>
              <td class="px-6 py-4 whitespace-nowrap">{{ product.region }}</td>
              <td class="px-6 py-4 whitespace-nowrap">{{ product.quantity }}</td>
              <td class="px-6 py-4 whitespace-nowrap">${{ product.price.toFixed(2) }}</td>
              <td class="px-6 py-4 whitespace-nowrap">{{ product.client }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-right">
                <button @click="deleteProduct(product._id)" class="text-red-600 hover:text-red-900">
                  <span class="material-icons-outlined">delete</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div
        class="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6 mt-4 rounded-lg shadow-sm">
        <div class="flex-1 flex justify-between sm:hidden">
          <button @click="previousPage" :disabled="currentPage === 1"
            class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            :class="{ 'opacity-50 cursor-not-allowed': currentPage === 1 }">
            Previous
          </button>
          <button @click="nextPage" :disabled="currentPage === totalPages"
            class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            :class="{ 'opacity-50 cursor-not-allowed': currentPage === totalPages }">
            Next
          </button>
        </div>
        <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
          <div>
            <p class="text-sm text-gray-700">
              Showing
              <span class="font-medium">{{ startItem }}</span>
              to
              <span class="font-medium">{{ endItem }}</span>
              of
              <span class="font-medium">{{ totalItems }}</span>
              results
            </p>
          </div>
          <div>
            <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
              <button @click="previousPage" :disabled="currentPage === 1"
                class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                :class="{ 'opacity-50 cursor-not-allowed': currentPage === 1 }">
                <span class="material-icons-outlined">chevron_left</span>
              </button>
              <button v-for="page in displayedPages" :key="page" @click="goToPage(page)"
                class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium"
                :class="page === currentPage ? 'z-10 bg-blue-50 border-blue-500 text-blue-600' : 'bg-white text-gray-500 hover:bg-gray-50'">
                {{ page }}
              </button>
              <button @click="nextPage" :disabled="currentPage === totalPages"
                class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                :class="{ 'opacity-50 cursor-not-allowed': currentPage === totalPages }">
                <span class="material-icons-outlined">chevron_right</span>
              </button>
            </nav>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'

export default {
  name: 'ListView',
  setup() {
    const products = ref([])
    const showAddForm = ref(false)
    const currentPage = ref(1)
    const itemsPerPage = ref(10)
    const totalItems = ref(0)
    const sortColumn = ref('name')
    const sortDirection = ref('asc')
    const filters = ref({
      search: '',
      category: '',
      region: ''
    })

    const tableHeaders = [
      { key: 'name', label: 'Name' },
      { key: 'category', label: 'Category' },
      { key: 'region', label: 'Region' },
      { key: 'quantity', label: 'Quantity' },
      { key: 'price', label: 'Price' },
      { key: 'client', label: 'Client' }
    ]

    const categories = ref(['Electronics', 'Clothing', 'Food', 'Books'])
    const regions = ref(['North', 'South', 'East', 'West'])

    const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value))
    const startItem = computed(() => ((currentPage.value - 1) * itemsPerPage.value) + 1)
    const endItem = computed(() => Math.min(currentPage.value * itemsPerPage.value, totalItems.value))

    const displayedPages = computed(() => {
      const delta = 2
      const range = []
      const rangeWithDots = []
      let l

      for (let i = 1; i <= totalPages.value; i++) {
        if (i === 1 || i === totalPages.value || (i >= currentPage.value - delta && i <= currentPage.value + delta)) {
          range.push(i)
        }
      }

      range.forEach(i => {
        if (l) {
          if (i - l === 2) {
            rangeWithDots.push(l + 1)
          } else if (i - l !== 1) {
            rangeWithDots.push('...')
          }
        }
        rangeWithDots.push(i)
        l = i
      })

      return rangeWithDots
    })

    const fetchProducts = async () => {
      try {
        const response = await fetch('http://localhost:8888/private/product/list', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            query: buildQuery(),
            options: {
              page: currentPage.value,
              limit: itemsPerPage.value,
              sort: { [sortColumn.value]: sortDirection.value === 'asc' ? 1 : -1 },
              filters: buildFilters()
            }
          })
        })

        const data = await response.json()
        products.value = data.data
        totalItems.value = data.pagination.total
      } catch (error) {
        console.error('Error fetching products:', error)
      }
    }


    const buildQuery = () => {
      const query = {}
      if (filters.value.search) {
        query.$or = [
          { name: { $regex: filters.value.search, $options: 'i' } },
          { category: { $regex: filters.value.search, $options: 'i' } },
          { client: { $regex: filters.value.search, $options: 'i' } }
        ]
      }
      return query
    }

    const buildFilters = () => {
      const filtersObj = {}
      if (filters.value.category) filtersObj.category = filters.value.category
      if (filters.value.region) filtersObj.region = filters.value.region
      return filtersObj
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchProducts()
    }

    const sortBy = (column) => {
      if (sortColumn.value === column) {
        sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
      } else {
        sortColumn.value = column
        sortDirection.value = 'asc'
      }
      fetchProducts()
    }

    const deleteProduct = async (productId) => {
      try {
        await fetch('http://localhost:8888/private/product/remove', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ productId })
        })
        fetchProducts()
      } catch (error) {
        console.error('Error deleting product:', error)
      }
    }

    const confirmRemoveAll = async () => {
  if (confirm('Are you sure you want to remove all products? This action cannot be undone.')) {
    try {
      await fetch('http://localhost:8888/private/product/removeAll', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      fetchProducts();
    } catch (error) {
      console.error('Error removing all products:', error);
    }
  }
}

    const handleProductAdded = () => {
      showAddForm.value = false
      fetchProducts()
    }

    const goToPage = (page) => {
      currentPage.value = page
      fetchProducts()
    }

    const previousPage = () => {
      if (currentPage.value > 1) {
        currentPage.value--
        fetchProducts()
      }
    }

    const nextPage = () => {
      if (currentPage.value < totalPages.value) {
        currentPage.value++
        fetchProducts()
      }
    }

    onMounted(() => {
      fetchProducts()
    })

    return {
      products,
      showAddForm,
      currentPage,
      totalItems,
      itemsPerPage,
      sortColumn,
      sortDirection,
      filters,
      tableHeaders,
      categories,
      regions,
      totalPages,
      startItem,
      endItem,
      displayedPages,
      handleSearch,
      sortBy,
      deleteProduct,
      handleProductAdded,
      goToPage,
      previousPage,
      nextPage,
      fetchProducts
    }
  }
}
</script>
