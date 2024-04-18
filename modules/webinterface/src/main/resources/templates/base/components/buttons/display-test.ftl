<#import "../../body.ftl" as body>

<#import "default-buttons.ftl" as defaultButtons>
<#import "light-buttons.ftl" as lightButtons>
<#import "dark-buttons.ftl" as darkButtons>
<#import "success-buttons.ftl" as successButtons>
<#import "warning-buttons.ftl" as warningButtons>
<#import "danger-buttons.ftl" as dangerButtons>

<@body.default>
    <div class="m-3">
        <@defaultButtons.sm>
            Default (SM)
        </@defaultButtons.sm>
        <@defaultButtons.md>
            Default (MD)
        </@defaultButtons.md>
        <@defaultButtons.lg>
            Default (LG)
        </@defaultButtons.lg>
    </div>
    <div class="m-3">
        <@lightButtons.sm>
            Light (SM)
        </@lightButtons.sm>
        <@lightButtons.md>
            Light (MD)
        </@lightButtons.md>
        <@lightButtons.lg>
            Light (LG)
        </@lightButtons.lg>
    </div>
    <div class="m-3">
        <@darkButtons.sm>
            Dark (SM)
        </@darkButtons.sm>
        <@darkButtons.md>
            Dark (MD)
        </@darkButtons.md>
        <@darkButtons.lg>
            Dark (LG)
        </@darkButtons.lg>
    </div>
    <div class="m-3">
        <@successButtons.sm>
            Success (SM)
        </@successButtons.sm>
        <@successButtons.md>
            Success (MD)
        </@successButtons.md>
        <@successButtons.lg>
            Success (LG)
        </@successButtons.lg>
    </div>
    <div class="m-3">
        <@warningButtons.sm>
            Warning (SM)
        </@warningButtons.sm>
        <@warningButtons.md>
            Warning (MD)
        </@warningButtons.md>
        <@warningButtons.lg>
            Warning (LG)
        </@warningButtons.lg>
    </div>
    <div class="m-3">
        <@dangerButtons.sm>
            Danger (SM)
        </@dangerButtons.sm>
        <@dangerButtons.md>
            Danger (MD)
        </@dangerButtons.md>
        <@dangerButtons.lg>
            Danger (LG)
        </@dangerButtons.lg>
    </div>
</@body.default>