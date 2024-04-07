package cloud

import org.testcontainers.images.builder.ImageFromDockerfile

val NODE_IMAGE_NAME = "redicloud-node"
val NODE_IMAGE_BUILD = ImageFromDockerfile(NODE_IMAGE_NAME, false)
    .withDockerfileFromBuilder {
        it.from("openjdk:17-alpine")
            .run("apk add screen")
            .build()
    }
val NODE_IMAGE = "redicloud-node:latest"