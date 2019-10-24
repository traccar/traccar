<template>
  <q-layout view="hHh lpR fFf">
    <MainHeader />
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script>
import MainHeader from "../components/layouts/MainHeader";

export default {
  name: "default",
  components: {
    MainHeader
  },
  data() {
    return {
      leftDrawerOpen: false
    };
  }
};
</script>
