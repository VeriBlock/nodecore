## Overview
The bootstrap-downloader is a new tool which can be used to download all block files, or missing block files, and an updated nodecore.dat from the VeriBlock mirrors, so that you can sync a full node much quicker than from peers. When doing an incremental update, it checks all existing block files for corruption and will replace them along with downloading any missing (newer) files that you may not have. 

## Usage
The bootstrap-downloader can be started with the **bootstrap-downloader.bat** (windows) and **bootstrap-downloader** (linux/mac)

By default, the bootstrap-downloader will download the **mainnet** block files inside the **nodecore/bin** folder from the **distributed package**.

If for any reason the **nodecore/bin** folder from the distributed package can't be located (e.g: you call the start script from another folder) the data blocks will be downloaded at **./**

### Optional Parameters
|Parameter|Description                      |Example          |
|---------|---------------------------------|-----------------|
|-d       |Specify the target data directory|-d /nodecore_data|
|-n       |Specify the target network       |-n testnet       |

### Examples
* To download the **testnet** blocks inside the  **nodecore/bin** folder from the **distributed package**: ``bootstrap-downloader.bat -n testnet``
* To download the **testnet** blocks inside a custom data folder: ``bootstrap-downloader.bat -n testnet -d route_to_data_folder``
* To download the **mainnet** blocks inside a custom data folder: ``bootstrap-downloader.bat -d route_to_data_folder``
