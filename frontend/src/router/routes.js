const routes = [
  {
    path: "/",
    component: () => import("layouts/default.vue"),
    children: [{ path: "", component: () => import("pages/Index.vue") }]
  },
  {
    path: "/login",
    component: () => import("layouts/public.vue"),
    children: [{ path: "", component: () => import("pages/Login.vue") }]
  }
];

// Always leave this as last one
if (process.env.MODE !== "ssr") {
  routes.push({
    path: "*",
    component: () => import("pages/Error404.vue")
  });
}

export default routes;
