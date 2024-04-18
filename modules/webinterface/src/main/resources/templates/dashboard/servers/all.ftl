<#import "../../base/dashboard.ftl" as dashboard>
<#assign title = "Servers" in dashboard>

<@dashboard.default>

<div id="accordion-flush" data-accordion="open" data-active-classes="font-semibold" data-inactive-classes="font-normal" class="text-gray-900">

    <h2 id="accordion-flush-heading-1">
        <button type="button" class="flex items-center justify-between w-full py-5 text-2xl rtl:text-right text-black border-b border-gray-200 dark:border-gray-700 dark:text-gray-400 gap-3 dark:text-white" data-accordion-target="#accordion-flush-body-1" aria-expanded="true" aria-controls="accordion-flush-body-1">
            <span>
                Lobby
                <span class="bg-green-100 text-green-800 text-xs font-medium mx-1 px-2.5 py-0.5 rounded dark:bg-green-900 dark:text-green-300">
                    3 connected
                </span>
                <span class="bg-gray-100 text-gray-800 text-xs font-medium mx-1 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                    105 / 200 players
                </span>
                <span class="bg-gray-100 text-gray-800 text-xs font-medium mx-1 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                    dynamic
                </span>
            </span>
            <svg data-accordion-icon class="w-3 h-3 rotate-180 shrink-0" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 10 6">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5 5 1 1 5"/>
            </svg>
        </button>
    </h2>
    <div id="accordion-flush-body-1" class="hidden py-5" aria-labelledby="accordion-flush-heading-1">
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5 gap-3">
            <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white">
                <div class="flex items-center justify-between">
                    <span><b>Lobby-1</b></span>
                    <span class="bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-green-900 dark:text-green-300">running</span>
                </div>
                <hr class="my-3">
                <div class="grid grid-cols-2 gap-y-2">
                    <span>Node</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            node01
                        </span>
                    </div>
                    <span>IP-Address</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            37.114.156.123:5002
                        </span>
                    </div>
                    <span>RAM usage</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            1744 / 2048 MB
                        </span>
                    </div>
                    <span>Players</span>
                    <div class="flex justify-end">
                        <span class="bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-yellow-900 dark:text-yellow-300">
                            47 / 50
                        </span>
                    </div>
                </div>
                <hr class="my-3">
                <div class="flex justify-between">
                    <button type="button" class="text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-blue-600 dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800">
                        View
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900">
                        Stop
                    </button>
                </div>
            </div>

            <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white">
                <div class="flex items-center justify-between">
                    <span><b>Lobby-2</b></span>
                    <span class="bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-green-900 dark:text-green-300">running</span>
                </div>
                <hr class="my-3">
                <div class="grid grid-cols-2 gap-y-2">
                    <span>Node</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            node01
                        </span>
                    </div>
                    <span>IP-Address</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            37.114.156.123:5003
                        </span>
                    </div>
                    <span>RAM usage</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            1233 / 2048 MB
                        </span>
                    </div>
                    <span>Players</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            27 / 50
                        </span>
                    </div>
                </div>
                <hr class="my-3">
                <div class="flex justify-between">
                    <button type="button" class="text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-blue-600 dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800">
                        View
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900">
                        Stop
                    </button>
                </div>
            </div>

            <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white">
                <div class="flex items-center justify-between">
                    <span><b>Lobby-3</b></span>
                    <span class="bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-green-900 dark:text-green-300">running</span>
                </div>
                <hr class="my-3">
                <div class="grid grid-cols-2 gap-y-2">
                    <span>Node</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            node01
                        </span>
                    </div>
                    <span>IP-Address</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            37.114.156.123:5003
                        </span>
                    </div>
                    <span>RAM usage</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            1432 / 2048 MB
                        </span>
                    </div>
                    <span>Players</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            31 / 50
                        </span>
                    </div>
                </div>
                <hr class="my-3">
                <div class="flex justify-between">
                    <button type="button" class="text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-blue-600 dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800">
                        View
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900">
                        Stop
                    </button>
                </div>
            </div>

            <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white">
                <div class="flex items-center justify-between">
                    <span><b>Lobby-4</b></span>
                    <span class="bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-yellow-900 dark:text-yellow-300">queued</span>
                </div>
                <hr class="my-3">
                <div class="grid grid-cols-2 gap-y-2">
                    <span>Node</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            node01
                        </span>
                    </div>
                    <span>IP-Address</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            37.114.156.123:5005
                        </span>
                    </div>
                    <span>RAM usage</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            0 / 2048 MB
                        </span>
                    </div>
                    <span>Players</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            0 / 50
                        </span>
                    </div>
                </div>
                <hr class="my-3">
                <div class="flex justify-between">
                    <button type="button" class="text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-blue-600 dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800">
                        View
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900">
                        Stop
                    </button>
                </div>
            </div>
        </div>
    </div>

    <h2 id="accordion-flush-heading-2">
        <button type="button" class="flex items-center justify-between w-full py-5 text-2xl font-semibold rtl:text-right text-black border-b border-gray-200 dark:border-gray-700 dark:text-gray-400 gap-3 dark:text-white" data-accordion-target="#accordion-flush-body-2" aria-expanded="true" aria-controls="accordion-flush-body-2">
            <span>
                BuildServer
                <span class="bg-green-100 text-green-800 text-xs font-medium mx-1 px-2.5 py-0.5 rounded dark:bg-green-900 dark:text-green-300">
                    1 connected
                </span>
                <span class="bg-gray-100 text-gray-800 text-xs font-medium mx-1 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                    3 / 20 players
                </span>
                <span class="bg-gray-100 text-gray-800 text-xs font-medium mx-1 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                    static
                </span>
            </span>
            <svg data-accordion-icon class="w-3 h-3 rotate-180 shrink-0" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 10 6">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5 5 1 1 5"/>
            </svg>
        </button>
    </h2>
    <div id="accordion-flush-body-2" class="hidden py-5" aria-labelledby="accordion-flush-heading-2">
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5 gap-3">
            <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white">
                <div class="flex items-center justify-between">
                    <span><b>BuildServer-1</b></span>
                    <span class="bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-green-900 dark:text-green-300">running</span>
                </div>
                <hr class="my-3">
                <div class="grid grid-cols-2 gap-y-2">
                    <span>Node</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            node01
                        </span>
                    </div>
                    <span>IP-Address</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            37.114.156.123:5006
                        </span>
                    </div>
                    <span>RAM usage</span>
                    <div class="flex justify-end">
                        <span class="bg-red-100 text-red-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-red-900 dark:text-red-300">
                            2010 / 2048 MB
                        </span>
                    </div>
                    <span>Players</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            3 / 20
                        </span>
                    </div>
                </div>
                <hr class="my-3">
                <div class="flex justify-between">
                    <button type="button" class="text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-blue-600 dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800">
                        View
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900">
                        Stop
                    </button>
                </div>
            </div>
            <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white">
                <div class="flex items-center justify-between">
                    <span><b>BuildServer-2</b></span>
                    <span class="bg-red-100 text-red-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-red-900 dark:text-red-300">stopped</span>
                </div>
                <hr class="my-3">
                <div class="grid grid-cols-2 gap-y-2">
                    <span>Node</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            node02
                        </span>
                    </div>
                    <span>IP-Address</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            ----
                        </span>
                    </div>
                    <span>RAM usage</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            0 / 4096 MB
                        </span>
                    </div>
                    <span>Players</span>
                    <div class="flex justify-end">
                        <span class="bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-gray-700 dark:text-gray-300">
                            0 / 20
                        </span>
                    </div>
                </div>
                <hr class="my-3">
                <div class="flex justify-between">
                    <button type="button" class="text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-blue-600 dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800">
                        View
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-green-700 hover:bg-green-800 focus:ring-4 focus:ring-green-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-green-600 dark:hover:bg-green-700 dark:focus:ring-green-800">
                        Start
                    </button>
                    <button type="button" class="focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-2.5 py-1 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900">
                        Delete
                    </button>
                </div>
            </div>
        </div>
    </div>

</div>

    <!--
<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5 gap-3">
    <div class="bg-white rounded-lg shadow dark:border dark:bg-gray-800 dark:border-gray-700 p-4 dark:text-white ">

    </div>
</div>
-->
</@dashboard.default>