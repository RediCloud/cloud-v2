<#import "../base/html.ftl" as html>
<#import "../base/head.ftl" as head>
<#import "../base/body.ftl" as body>
<#import "../base/navbar.ftl" as navbar>
<#import "../base/sidebar.ftl" as sidebar>

<#macro default>
    <@html.default>
        <@head.default>
            <title>${title}</title>
        </@head.default>
        <@body.default>
            <@navbar.default/>
            <@sidebar.default/>
            <#nested />
        </@body.default>
    </@html.default>
</#macro>