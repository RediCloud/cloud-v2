<#import "base/html.ftl" as html>
<#import "base/head.ftl" as head>
<#import "base/body.ftl" as body>

<@html.default>
    <@head.default>
        <title>Home</title>
    </@head.default>
    <@body.default>
        <div class="flex
                flex-col
                items-center
                justify-center
                px-6
                py-8
                mx-auto
                md:h-screen
                lg:py-0">
            <div class="flex
                    items-center
                    mb-6
                    text-4xl
                    font-semibold
                    text-gray-900
                    dark:text-white">
                <img src="/assets/img/logo.png" alt="Logo" width="100" height="100">
                <span>Welcome</span>
            </div>
            <div class="w-full
                    bg-white
                    rounded-lg
                    shadow
                    dark:border
                    md:mt-0
                    sm:max-w-md
                    xl:p-0
                    dark:bg-gray-800
                    dark:border-gray-700">
                <div class="p-6 space-y-4 md:space-y-6 sm:p-8">
                    <form class="space-y-4 md:space-y-6" action="/auth/login" method="post">
                    <span class="text-xl
                                font-bold
                                leading-tight
                                tracking-tight
                                text-gray-900
                                md:text-2xl
                                dark:text-white">Log in!</span>
                        <p class="dark:text-white">Please enter your credentials</p>
                        <div>
                            <label for="username" class="
                                    block
                                    mb-2
                                    text-sm
                                    font-medium
                                    text-gray-900
                                    dark:text-white">Username</label>
                            <input type="text" name="username" id="username" class="bg-gray-50
                                                                                border
                                                                                border-gray-300
                                                                                text-gray-900
                                                                                sm:text-sm
                                                                                rounded-lg
                                                                                focus:outline-none
                                                                                focus:ring-2
                                                                                focus:ring-sky-600
                                                                                focus:border-sky-600
                                                                                block
                                                                                w-full
                                                                                p-2.5
                                                                                dark:bg-gray-700
                                                                                dark:border-gray-600
                                                                                dark:placeholder-gray-400
                                                                                dark:text-white
                                                                                dark:focus:outline-none
                                                                                dark:focus:ring-2
                                                                                dark:focus:ring-blue-500
                                                                                dark:focus:border-blue-500">
                        </div>
                        <div>
                            <label for="password" class="block mb-2 text-sm font-medium text-gray-900 dark:text-white">Password</label>
                            <input type="password" name="password" id="password" class="bg-gray-50
                                                                                border
                                                                                border-gray-300
                                                                                text-gray-900
                                                                                sm:text-sm
                                                                                rounded-lg
                                                                                focus:outline-none
                                                                                focus:ring-2
                                                                                focus:ring-sky-600
                                                                                focus:border-sky-600
                                                                                block
                                                                                w-full
                                                                                p-2.5
                                                                                dark:bg-gray-700
                                                                                dark:border-gray-600
                                                                                dark:placeholder-gray-400
                                                                                dark:text-white
                                                                                dark:focus:outline-none
                                                                                dark:focus:ring-2
                                                                                dark:focus:ring-blue-500
                                                                                dark:focus:border-blue-500">
                        </div>
                        <button type="submit" class="w-full text-white bg-sky-600 hover:bg-sky-700 focus:ring-4 focus:outline-none focus:ring-sky-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center dark:bg-sky-600 dark:hover:bg-sky-700 dark:focus:ring-sky-800">
                            Login
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </@body.default>
</@html.default>