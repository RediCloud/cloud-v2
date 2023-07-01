group = "dev.redicloud.repository.template.file"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":file-cluster"))
    dependency("com.jcraft:jsch:0.1.55")
}