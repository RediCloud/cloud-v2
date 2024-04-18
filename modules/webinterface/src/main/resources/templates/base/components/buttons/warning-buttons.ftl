<#import "buttons.ftl" as buttons>

<#assign btn_warning_bg_color = "yellow-700">
<#assign btn_warning_hover_bg_color = "yellow-800">
<#assign btn_warning_focus_bg_color = "yellow-300 ">

<#assign btn_warning_dark_bg_color = "yellow-600">
<#assign btn_warning_dark_hover_bg_color = "yellow-700">
<#assign btn_warning_dark_focus_bg_color = "yellow-800">

<#macro sm>
    <button type="button" class="focus:outline-none text-white bg-${btn_warning_bg_color} hover:bg-${btn_warning_hover_bg_color} focus:ring-${btn_warning_focus_bg_color}
            focus:ring-4 font-medium rounded-lg text-sm px-${buttons.btn_sm_px} py-${buttons.btn_sm_py} me-${buttons.btn_sm_me} mb-${buttons.btn_sm_mb}
            dark:bg-${btn_warning_dark_bg_color} dark:hover:bg-${btn_warning_dark_hover_bg_color} focus:outline-none dark:focus:ring-${btn_warning_dark_focus_bg_color}">
        <#nested />
    </button>
</#macro>

<#macro md>
    <button type="button" class="focus:outline-none text-white bg-${btn_warning_bg_color} hover:bg-${btn_warning_hover_bg_color} focus:ring-${btn_warning_focus_bg_color}
            focus:ring-4 font-medium rounded-lg text-sm px-${buttons.btn_md_px} py-${buttons.btn_md_py} me-${buttons.btn_md_me} mb-${buttons.btn_md_mb}
            dark:bg-${btn_warning_dark_bg_color} dark:hover:bg-${btn_warning_dark_hover_bg_color} focus:outline-none dark:focus:ring-${btn_warning_dark_focus_bg_color}">
        <#nested />
    </button>
</#macro>

<#macro lg>
    <button type="button" class="focus:outline-none text-white bg-${btn_warning_bg_color} hover:bg-${btn_warning_hover_bg_color} focus:ring-${btn_warning_focus_bg_color}
            focus:ring-4 font-medium rounded-lg text-sm px-${buttons.btn_lg_px} py-${buttons.btn_lg_py} me-${buttons.btn_lg_me} mb-${buttons.btn_lg_mb}
            dark:bg-${btn_warning_dark_bg_color} dark:hover:bg-${btn_warning_dark_hover_bg_color} focus:outline-none dark:focus:ring-${btn_warning_dark_focus_bg_color}">
        <#nested />
    </button>
</#macro>