# VeriBlock SPV

## Proof-of-Proof (PoP) Overview 

Proof-of-Proof (PoP) is an abstract consensus algorithm that enables a blockchain to inherit another's Proof-of-Work (PoW) security. VeriBlock is a concrete implementation of a blockchain secured by PoP, and which is purpose-built to provide low-cost and low-friction adoption of PoP for any other blockchain (secured to Bitcoin) without incurring significant technical debt/limitations.

## SPV Overview

Because VeriBlock employs an account-based model (rather than the UTXO model used by Bitcoin and derivatives), SPV behaves a little differently. SPV on VeriBlock also benefits from the Bitcoin-level security the VeriBlock network has; compact objects which describe the state of the VeriBlock and Bitcoin blockchains, and the relevant VeriBlock block header publications in Bitcoin allow the SPV client to perform the same PoP-based fork resolution protocol that full nodes employ.

Because a ledger proof (which proves an address's current balance and signature index) can be produced for any VeriBlock block, an SPV node can detect whether the node(s) it is communicating with to get network state are exerting censorship by comparing the transactions the full node(s) return versus the balance proofs they return.

## Getting started

To get started, all you need is a version of the JDK 1.8 or higher.

### Building from the command line

The following commands are for Mac/Linux. For Windows, use the .bat variants

To perform a full build, run the following

`$ gradle clean build`

## Configuration Context

You have to initialize Context on the start of the application.

`veriblock.Context.init(..,..,..)`
