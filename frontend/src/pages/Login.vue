<template>
  <q-page class="flex flex-center">
    <div class="q-pa-md login_base">
      <q-form @submit="onSubmit" class="q-gutter-md">
        <q-card class="login_styles">
          <q-card-section></q-card-section>
          <q-card-section>
            <q-input
              rounded
              outlined
              bg-color="grey-2 text-red-10"
              :input-style="{ borderColor: '#f445d3' }"
              color="red-10 text-red-10"
              v-model="username"
              label="Username"
              lazy-rules
              :rules="[
                val => (val && val.length > 0) || 'Please type username..'
              ]"
            >
              <template v-slot:prepend>
                <q-icon color="red-10" name="person" />
              </template>
            </q-input>

            <q-input
              rounded
              outlined
              bg-color="grey-2 text-red-10"
              color="red-10"
              type="password"
              v-model="password"
              label="Password"
              lazy-rules
              :rules="[
                val =>
                  (val !== null && val !== '') || 'Please type your password'
              ]"
            >
              <template v-slot:prepend>
                <q-icon color="red-10" name="lock" />
              </template>
            </q-input>
          </q-card-section>
          <q-card-actions>
            <q-toggle color="red-10" v-model="remember" label="Remember me." />
            <q-space />
            <q-btn
              label="Sign In"
              type="submit"
              style="padding: 0px 25px 0px 25px; margin-rigth: 0px;"
              color="red-10"
              no-caps
              unelevated
              rounded
              dense
            />
          </q-card-actions>
          <q-card-section></q-card-section>
        </q-card>
        <div></div>
      </q-form>
    </div>
  </q-page>
</template>

<style></style>

<script>
export default {
  name: "Login",
  data() {
    return {
      username: null,
      password: null,

      remember: true
    };
  },

  methods: {
    onSubmit() {
      if (this.remember !== true) {
        this.$q.notify({
          color: "red-5",
          textColor: "white",
          icon: "fas fa-exclamation-triangle",
          message: "You need to accept the license and terms first"
        });
      } else {
        this.$q.notify({
          color: "green-4",
          textColor: "white",
          icon: "fas fa-check-circle",
          message: "Submitted"
        });
      }
    }
  }
};
</script>

<style>
.login_styles {
  min-width: 400px;
}

.login_base {
  margin-top: -10vh;
}
</style>
