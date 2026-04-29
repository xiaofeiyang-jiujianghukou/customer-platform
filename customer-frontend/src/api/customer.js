import axios from 'axios'

const api = axios.create({
  baseURL: '/api/customers',
  timeout: 30000
})

export function queryCustomers(params) {
  return api.post('/page', params)
}

export function getCustomer(customerNo) {
  return api.get(`/${customerNo}`)
}

export function updateCustomer(customerNo, data) {
  return api.put(`/${customerNo}`, data)
}

export function generateTestData() {
  return api.post('/generate-test-data')
}
