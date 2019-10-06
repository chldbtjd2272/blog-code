<template>
    <div>
        <p>감사합니다22!1!!</p>
        <input type="text" v-model="text">
        <button @click="onClick"> 클릭 </button>
    </div>
</template>

<script>
    export default {
        name: "HomeComponent",
        data(){
            return{
                text:""
            }
        },
        methods:{
            onClick(){
                console.error("@");
                console.log("!@");
            }
        }
    }
</script>

<style scoped>

</style>