<#import "../base/dashboard.ftl" as dashboard>
<#assign title = "Home" in dashboard>

<@dashboard.default>
    <div class="flex justify-center">
        <div class="w-full md:w-1/2 lg:w-1/3">
            <div class="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4">
                <h1 class="text-2xl font-bold mb-4">Welcome to the Dashboard</h1>
                <p class="text-gray-700 text-base">This is a simple dashboard template for your project.</p>
            </div>
        </div>
    </div>
</@dashboard.default>