import Vue from 'vue'
import App from './App.vue'
import BootStrap from 'bootstrap-vue';
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

Vue.use(BootStrap);
Vue.config.productionTip = false;

new Vue({
  render: h => h(App),
}).$mount('#app');
