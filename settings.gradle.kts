
include("veriblock-core")
include("veriblock-shell")

include("nodecore-grpc")
include("nodecore-ucp")
include("nodecore-p2p")
include("nodecore-cli")
include("veriblock-pow-miner")
include("veriblock-extensions")

include("altchain-sdk")

include("pop-miners:pop-miners-common")
include("pop-miners:veriblock-pop-miners-common")
include("pop-miners:veriblock-pop-miner")
include("pop-miners:altchain-pop-miner")

include("altchain-plugins")
include("nodecore-spv")

rootProject.name = "nodecore-suite"
