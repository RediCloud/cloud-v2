<#import "../base/dashboard.ftl" as dashboard>
<#assign title = "Home" in dashboard>

<@dashboard.default>
    <div class="flex justify-center">
        <div class="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4">
            <h1 class="text-2xl font-bold mb-4">Welcome to the Dashboard</h1>
            <p class="text-gray-700 text-base">This is a simple dashboard to manage your cluster!</p>
        </div>
    </div>
</@dashboard.default>