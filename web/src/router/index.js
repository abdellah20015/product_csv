import HomeView from '@/views/HomeView.vue'
import FormView from '@/views/Products/FormView.vue'
import ListView from '@/views/Products/ListView.vue'
import { createRouter, createWebHistory } from 'vue-router'



const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/products',
    name: 'ListProducts',
    component: ListView
  },
  {
    path: '/products/add',
    name: 'AddProduct',
    component: FormView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
