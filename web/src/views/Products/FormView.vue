<!-- ProductForm.vue -->
<template>
  <div class="min-h-screen bg-gray-100 py-8">
    <div class="max-w-2xl mx-auto bg-white rounded-lg shadow-md">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
        <h3 class="text-lg font-medium text-gray-900">Add New Product</h3>
        <router-link
          to="/products"
          class="text-gray-400 hover:text-gray-500"
        >
          <span class="material-icons-outlined">close</span>
        </router-link>
      </div>

      <!-- Mode Switch -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-center">
          <div class="inline-flex rounded-lg p-1 bg-gray-100">
            <button
              @click="mode = 'form'"
              :class="[
                'px-4 py-2 rounded-md text-sm font-medium transition-all duration-200',
                mode === 'form'
                  ? 'bg-white shadow-sm text-gray-900'
                  : 'text-gray-500 hover:text-gray-900'
              ]"
            >
              <div class="flex items-center space-x-2">
                <span>Manual Form</span>
              </div>
            </button>
            <button
              @click="mode = 'excel'"
              :class="[
                'px-4 py-2 rounded-md text-sm font-medium transition-all duration-200',
                mode === 'excel'
                  ? 'bg-white shadow-sm text-gray-900'
                  : 'text-gray-500 hover:text-gray-900'
              ]"
            >
              <div class="flex items-center space-x-2">
                <span class="material-icons-outlined text-sm">table_view</span>
                <span>Excel Upload</span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Excel Upload Form -->
      <div v-if="mode === 'excel'" class="px-6 py-4">
        <div class="flex items-center justify-center w-full">
          <label class="flex flex-col items-center justify-center w-full h-64 border-2 border-gray-300 border-dashed rounded-lg cursor-pointer bg-gray-50 hover:bg-gray-100">
            <div class="flex flex-col items-center justify-center pt-5 pb-6">
              <span class="material-icons-outlined text-4xl mb-2 text-gray-500">upload_file</span>
              <p class="mb-2 text-sm text-gray-500">
                <span class="font-semibold">Click to upload</span> or drag and drop
              </p>
              <p class="text-xs text-gray-500">Excel files only (CSV)</p>
            </div>
            <input
              type="file"
              class="hidden"
              accept=".csv,.xlsx,.xls"
              @change="handleFileUpload"
            />
          </label>
        </div>
        <div v-if="uploadStatus" :class="['mt-4 p-4 rounded-md',
          uploadStatus.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700']">
          {{ uploadStatus.message }}
        </div>
      </div>

      <!-- Form -->
      <form v-else @submit.prevent="handleSubmit" class="px-6 py-4">
        <div class="space-y-4">
          <!-- Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700">Name</label>
            <input
              v-model="formData.name"
              type="text"
              required
              class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              :class="{ 'border-red-500': errors.name }"
            />
            <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
          </div>

          <!-- Category -->
          <div>
            <label class="block text-sm font-medium text-gray-700">Category</label>
            <select
              v-model="formData.category"
              required
              class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              :class="{ 'border-red-500': errors.category }"
            >
              <option value="">Select a category</option>
              <option v-for="category in categories" :key="category" :value="category">
                {{ category }}
              </option>
            </select>
            <p v-if="errors.category" class="mt-1 text-sm text-red-600">{{ errors.category }}</p>
          </div>

          <!-- Region -->
          <div>
            <label class="block text-sm font-medium text-gray-700">Region</label>
            <select
              v-model="formData.region"
              required
              class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              :class="{ 'border-red-500': errors.region }"
            >
              <option value="">Select a region</option>
              <option v-for="region in regions" :key="region" :value="region">
                {{ region }}
              </option>
            </select>
            <p v-if="errors.region" class="mt-1 text-sm text-red-600">{{ errors.region }}</p>
          </div>

          <!-- Price -->
          <div>
            <label class="block text-sm font-medium text-gray-700">Price</label>
            <div class="mt-1 relative rounded-md shadow-sm">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <span class="text-gray-500 sm:text-sm">$</span>
              </div>
              <input
                v-model.number="formData.price"
                type="number"
                step="0.01"
                required
                min="0"
                class="block w-full pl-7 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                :class="{ 'border-red-500': errors.price }"
              />
            </div>
            <p v-if="errors.price" class="mt-1 text-sm text-red-600">{{ errors.price }}</p>
          </div>

          <!-- Quantity -->
          <div>
            <label class="block text-sm font-medium text-gray-700">Quantity</label>
            <input
              v-model.number="formData.quantity"
              type="number"
              required
              min="0"
              class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              :class="{ 'border-red-500': errors.quantity }"
            />
            <p v-if="errors.quantity" class="mt-1 text-sm text-red-600">{{ errors.quantity }}</p>
          </div>

          <!-- Client -->
          <div>
            <label class="block text-sm font-medium text-gray-700">Client</label>
            <input
              v-model="formData.client"
              type="text"
              required
              class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              :class="{ 'border-red-500': errors.client }"
            />
            <p v-if="errors.client" class="mt-1 text-sm text-red-600">{{ errors.client }}</p>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="mt-6 flex justify-end space-x-3">
          <router-link
            to="/products"
            class="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Cancel
          </router-link>
          <button
            type="submit"
            :disabled="isSubmitting"
            class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <span v-if="isSubmitting">Saving...</span>
            <span v-else>Save Product</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

export default {
  name: 'ProductForm',

  setup() {
    const router = useRouter()
    const categories = ['Electronics', 'Clothing', 'Food', 'Books', 'Toys', 'Sports']
    const regions = ['North', 'South', 'East', 'West', 'Central']
    const isSubmitting = ref(false)
    const mode = ref('form')
    const uploadStatus = ref(null)

    const formData = reactive({
      name: '',
      category: '',
      region: '',
      price: '',
      quantity: '',
      client: ''
    })

    const errors = reactive({
      name: '',
      category: '',
      region: '',
      price: '',
      quantity: '',
      client: ''
    })

    const validateForm = () => {
      let isValid = true

      Object.keys(errors).forEach(key => errors[key] = '')

      if (!formData.name.trim()) {
        errors.name = 'Name is required'
        isValid = false
      }

      if (!formData.category) {
        errors.category = 'Category is required'
        isValid = false
      }

      if (!formData.region) {
        errors.region = 'Region is required'
        isValid = false
      }

      if (!formData.price || formData.price <= 0) {
        errors.price = 'Price must be greater than 0'
        isValid = false
      }

      if (!formData.quantity || formData.quantity < 0) {
        errors.quantity = 'Quantity must be 0 or greater'
        isValid = false
      }

      if (!formData.client.trim()) {
        errors.client = 'Client is required'
        isValid = false
      }

      return isValid
    }

    const handleSubmit = async () => {
      if (!validateForm()) return

      isSubmitting.value = true

      try {
        await axios.post('http://localhost:8888/private/product/insert', {
          name: formData.name,
          category: formData.category,
          region: formData.region,
          price: parseFloat(formData.price),
          quantity: parseInt(formData.quantity),
          client: formData.client
        })

        // Rediriger vers la liste des produits après succès
        router.push('/products')
      } catch (error) {
        console.error('Error adding product:', error)
        // Gérer l'erreur (vous pourriez ajouter une notification toast ici)
      } finally {
        isSubmitting.value = false
      }
    }

    const handleFileUpload = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      const formData = new FormData()
      formData.append('file', file)

      uploadStatus.value = null

      try {
        await axios.post('http://localhost:8888/private/file/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })

        uploadStatus.value = {
          type: 'success',
          message: 'File uploaded successfully!'
        }

        // Redirect after short delay
        setTimeout(() => {
          router.push('/products')
        }, 1500)
      } catch (error) {
        uploadStatus.value = {
          type: 'error',
          message: 'Error uploading file: ' + (error.response?.data?.message || error.message)
        }
      }
    }

    return {
      formData,
      errors,
      categories,
      regions,
      isSubmitting,
      handleSubmit,
      mode,
      handleFileUpload,
      uploadStatus
    }
  }
}
</script>
