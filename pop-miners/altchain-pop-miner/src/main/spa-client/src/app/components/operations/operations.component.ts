import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatSelectChange } from '@angular/material/select';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { startWith, switchMap } from 'rxjs/operators';
import { EMPTY, interval } from 'rxjs';

import { DataShareService } from '@core/services/data-share.service';
import { NetworkService } from '@core/services/network.service';
import { ConfigService } from '@core/services/config.service';
import { AlertService } from '@core/services/alert.service';
import { MinerService } from '@core/services/miner.service';

import { CoinConfigurationDialogComponent } from './coin-configuration-dialog/coin-configuration-dialog.component';
import { MineCustomBlockDialogComponent } from './mine-custom-block-dialog/mine-custom-block-dialog.component';

import {
  ConfiguredAltchain,
  AutoMineRound,
  OperationSummaryResponse,
  OperationDetailResponse,
  OperationWorkflow,
  OperationWorkflowStage,
  MineRequest,
  StateDetail,
  NetworkInfoResponse,
  ExplorerBaseUrlsResponse,
  MinerStatus,
} from '@core/model';
import { OperationState, OperationStatus } from '@core/enums';

@Component({
  selector: 'vbk-operations',
  templateUrl: './operations.component.html',
  styleUrls: ['./operations.component.scss'],
})
export class OperationsComponent implements OnInit, OnDestroy {
  public form: FormGroup = this.formBuilder.group({
    filter: 'Active',
  });

  public selectedAltChain: ConfiguredAltchain = null;
  public minerStatus: MinerStatus = null;
  public operationChain: string = null;
  public networkExplorerBaseUrl: ExplorerBaseUrlsResponse;

  public mineButtonReady: boolean = false;

  public filters: string[] = ['All', 'Active', 'Completed', 'Failed'];
  private globalOperationStages: string[] = [
    OperationState.INITIAL,
    OperationState.INSTRUCTION,
    OperationState.ENDORSEMENT_TRANSACTION,
    OperationState.ENDORSEMENT_TX_CONFIRMED,
    OperationState.BLOCK_OF_PROOF,
    OperationState.PROVEN,
    OperationState.SUBMITTED_POP_DATA,
    OperationState.PAYOUT_DETECTED,
    OperationState.COMPLETED,
  ];

  public isLoading = true;
  public tableLoading = false;
  public isLoadingConfiguration = false;
  public showLogo: boolean = false;

  public operationsTotalCount: number = 0;
  public selectedOperationId: string = null;

  public operationsDataSource =
    new MatTableDataSource<OperationSummaryResponse>();

  public pageLimit = 10;
  public pageOffset = 0;

  public operationWorkflows = {};
  public APIOperationWorkflows = {};

  private currentSelectionSubscription: any;
  private currentMinerStatusSubscription: any;

  constructor(
    private dataShareService: DataShareService,
    private networkService: NetworkService,
    private configService: ConfigService,
    private translate: TranslateService,
    private alertService: AlertService,
    private minerService: MinerService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.networkService
      .getNetworkInfo()
      .subscribe((data: NetworkInfoResponse) => {
        this.operationChain = data?.name === 'testnet' ? 'tVBK' : 'VBK';
        this.networkExplorerBaseUrl = data.explorerBaseUrls;
      });

    this.currentSelectionSubscription =
      this.dataShareService.currentAltChain.subscribe(
        (data: ConfiguredAltchain) => {
          if (JSON.stringify(data) !== JSON.stringify(this.selectedAltChain)) {
            const tempAltChain = { ...this.selectedAltChain };
            this.selectedAltChain = data;

            if (tempAltChain.id !== data.id || tempAltChain.key !== data.key) {
              this.initValues();
              this.updateQueryParams();
            }
          }
        }
      );

    this.currentMinerStatusSubscription =
      this.dataShareService.currentMinerStatus.subscribe(
        (data: MinerStatus) => {
          if (JSON.stringify(data) !== JSON.stringify(this.minerStatus)) {
            this.minerStatus = data;
          }
        }
      );

    // Get route's query params
    this.route.queryParams.subscribe((params) => {
      this.selectedOperationId = params.selectedOperationId || null;
      this.selectedAltChain.selectedFilter = params.statusFilter;

      if (this.selectedOperationId) {
        this.loadWorkFlow();
      }

      if (params.statusFilter) {
        this.form.controls['filter'].patchValue(params.statusFilter);
      }

      if (params.pageLimit) {
        this.pageLimit = parseInt(params.pageLimit, 10);
      }

      if (params.pageOffset) {
        this.pageOffset = parseInt(params.pageOffset, 10);
      }
    });

    // Check the operation list API every 2 seconds
    interval(2_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.minerService.getOperationList({
            altchainKey: this.selectedAltChain?.key || null,
            status:
              this.form.controls['filter']?.value?.toLowerCase() || 'active',
            limit: this.pageLimit,
            offset: this.pageOffset,
          })
        )
      )
      .subscribe((response) => {
        this.isLoading = false;
        if (
          JSON.stringify(this.operationsDataSource.data) ===
          JSON.stringify(response.operations)
        ) {
          return;
        }
        this.operationsDataSource.data = response.operations.slice();
        this.operationsTotalCount = response.totalCount;
      });

    // Check the operation details API every 5 seconds
    interval(5_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.selectedOperationId
            ? this.minerService.getOperation(this.selectedOperationId)
            : EMPTY
        )
      )
      .subscribe((data: OperationDetailResponse) => {
        if (
          JSON.stringify(this.APIOperationWorkflows[data.operationId]) ===
          JSON.stringify(data)
        ) {
          return;
        }
        this.APIOperationWorkflows[data.operationId] = data;
        this.operationWorkflows[data.operationId] = {
          ...this.createWorkflowTable(data),
        };
      });
  }

  ngOnDestroy(): void {
    this.currentSelectionSubscription.unsubscribe();
    this.currentMinerStatusSubscription.unsubscribe();
  }

  public initValues() {
    this.isLoading = true;
    this.tableLoading = false;
    this.isLoadingConfiguration = false;
    this.showLogo = false;

    this.operationsTotalCount = 0;
    this.selectedOperationId = null;

    this.operationsDataSource.data = [];

    this.pageLimit = 10;
    this.pageOffset = 0;

    this.operationWorkflows = {};

    this.form.controls['filter'].patchValue(
      this.selectedAltChain?.selectedFilter || 'Active'
    );
  }

  public loadWorkFlow() {
    this.minerService
      .getOperation(this.selectedOperationId)
      .subscribe((data: OperationDetailResponse) => {
        this.APIOperationWorkflows[data.operationId] = data;
        this.operationWorkflows[data?.operationId] = {
          ...this.createWorkflowTable(data),
        };
      });
  }

  public changeStatusFilter(event: MatSelectChange) {
    if (!event.value) {
      return;
    }
    this.updateQueryParams();
    this.refreshOperationList();
  }

  public pageChangeEmit(event: PageEvent) {
    let pageIndex = event.pageIndex;
    if (this.pageLimit !== event.pageSize) {
      pageIndex = 0;
    }
    this.pageLimit = event.pageSize;
    this.pageOffset = pageIndex * this.pageLimit;

    this.updateQueryParams();
    this.refreshOperationList();
  }

  private refreshOperationList() {
    this.tableLoading = Boolean(this.operationsDataSource?.data.length);

    this.minerService
      .getOperationList({
        altchainKey: this.selectedAltChain?.key || null,
        status: this.form.controls['filter']?.value || 'active',
        limit: this.pageLimit,
        offset: this.pageOffset,
      })
      .subscribe((response) => {
        this.operationsDataSource.data = response.operations.slice();

        this.operationsTotalCount = response.totalCount;
        this.tableLoading = false;
      });
  }

  private updateQueryParams() {
    const queryParams: Params = {};
    queryParams['selectedOperationId'] = this.selectedOperationId || null;
    queryParams['statusFilter'] =
      this.form.controls['filter']?.value &&
      this.form.controls['filter']?.value != 'Active'
        ? this.form.controls['filter']?.value
        : null;
    queryParams['pageLimit'] = this.pageLimit != 10 ? this.pageLimit : null;
    queryParams['pageOffset'] = this.pageOffset > 0 ? this.pageOffset : null;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: queryParams,
      queryParamsHandling: 'merge',
    });
  }

  public openCoinConfigurationDialog() {
    this.isLoadingConfiguration = true;

    this.configService.getAutoMineConfig('vbtc').subscribe((data) => {
      const dialogRef = this.dialog.open(CoinConfigurationDialogComponent, {
        width: '350px',
        panelClass: 'dialog',
        data: data,
        closeOnNavigation: true,
      });
      this.isLoadingConfiguration = false;

      dialogRef
        .afterClosed()
        .subscribe((result: { save: boolean; data: AutoMineRound[] }) => {
          if (result?.save) {
            this.configService
              .putAutoMineConfig('vbtc', {
                automineRounds: result?.data || null,
              })
              .subscribe(() => {
                this.alertService.addSuccess(
                  'Configuration updated successfully!'
                );
              });
          }
        });
    });
  }

  public openMineDialog() {
    this.isLoadingConfiguration = true;

    let defaultFeePerByte = null;
    let defaultMaxFee = null;

    this.configService
      .getVbkFee()
      .subscribe((data) => {
        defaultFeePerByte = data.feePerByte;
        defaultMaxFee = data.maxFee;

        const dialogRef = this.dialog.open(MineCustomBlockDialogComponent, {
          minWidth: '350px',
          maxWidth: '500px',
          panelClass: 'dialog',
          closeOnNavigation: true,
          data: {
            maxHeight: this.selectedAltChain?.syncStatus?.localBlockchainHeight,
            feePerByte: defaultFeePerByte,
            maxFee: defaultMaxFee,
          },
        });

        dialogRef
          .afterClosed()
          .subscribe(
            (result: {
              save: boolean;
              data: { maxHeight: number; feePerByte: number; maxFee: number };
            }) => {
              const { maxHeight, feePerByte, maxFee } = result?.data || {};

              if (!result?.save) return;

              if (
                defaultFeePerByte !== feePerByte ||
                defaultMaxFee !== maxFee
              ) {
                this.configService
                  .putVbkFee({ feePerByte, maxFee })
                  .subscribe(() => {
                    this.startMineBlock(maxHeight || null);
                  });
              } else {
                this.startMineBlock(maxHeight || null);
              }
            }
          );
      })
      .add(() => {
        this.isLoadingConfiguration = false;
      });
  }

  public startMineBlock(blockNumber?: number) {
    const request: MineRequest = {
      chainSymbol: this.selectedAltChain.key,
    };

    if (blockNumber) {
      request.height = blockNumber;
    }
    console.log(request, blockNumber);

    this.isLoadingConfiguration = true;

    this.minerService
      .postMine(request)
      .subscribe((response) => {
        this.alertService.addSuccess(
          'Mine request successful! Operation ID: ' + response.operationId
        );
      })
      .add(() => {
        this.isLoadingConfiguration = false;
      });
  }

  private createWorkflowTable(
    operation: OperationDetailResponse
  ): OperationWorkflow {
    const data: OperationWorkflow = {
      operationId: operation.operationId,
      stages: [],
    };

    const isFailed = Boolean(operation?.stateDetail?.failureReason);

    let currentStageNumber: number = this.globalOperationStages.findIndex(
      (x) => x.toLowerCase() === operation?.state?.toLowerCase()
    );

    if (currentStageNumber !== 0 && !currentStageNumber) {
      currentStageNumber = 8;
    }

    this.globalOperationStages.forEach((stage, index) => {
      const eachStage: OperationWorkflowStage = {
        status: this.getOperationStatus(stage, currentStageNumber, isFailed),
        taskName: `${index + 1}. ${this.globalOperationStages[index]}`,
        extraInformation: null,
      };

      eachStage.extraInformation = this.setStageExtraInformation(
        eachStage.status,
        operation,
        index
      );

      data.stages.push(eachStage);
    });
    return data;
  }

  private setStageExtraInformation(
    status: OperationStatus,
    operation: OperationDetailResponse,
    index: number
  ): string {
    switch (status) {
      case OperationStatus.DONE:
        return this.getExtraInformation(operation, index);

      case OperationStatus.ACTIVE:
        return this.getCurrentHint(operation, index);

      case OperationStatus.PENDING:
        return this.getPendingHint(operation, index);

      case OperationStatus.FAILED:
        return operation?.stateDetail?.failureReason;

      default:
        return '';
    }
  }

  private getOperationStatus(
    selectedState: string,
    currentStageNumber: number,
    isFailed: boolean
  ): OperationStatus {
    const selectedStateNumber: number = this.globalOperationStages.findIndex(
      (x) => x === selectedState
    );

    if (selectedStateNumber <= currentStageNumber) {
      return OperationStatus.DONE;
    }

    if (selectedStateNumber === currentStageNumber + 1) {
      return isFailed ? OperationStatus.FAILED : OperationStatus.ACTIVE;
    }

    return isFailed ? OperationStatus.EMPTY : OperationStatus.PENDING;
  }

  private getExtraInformation(
    operation: OperationDetailResponse,
    index: number
  ): string {
    switch (index) {
      case 0:
        return this.translate.instant('ApmOperationState_Done_Initial', {
          operationId: operation?.operationId,
        });

      case 1:
        return this.translate.instant('ApmOperationState_Done_Instruction', {
          operationChainName: this.selectedAltChain?.name,
          operationEndorsedBlockHeightHref:
            this.selectedAltChain?.explorerBaseUrls?.blockByHeight +
            operation?.endorsedBlockHeight.toString(),
          operationEndorsedBlockHeight: operation?.endorsedBlockHeight,
        });

      case 2:
        return this.translate.instant(
          'ApmOperationState_Done_Endorsement_Transaction',
          {
            contextVbkTokenName: this.operationChain,
            transactionTxHref:
              this.networkExplorerBaseUrl?.transaction +
              operation?.stateDetail?.vbkEndorsementTxId,
            transactionTxId: operation?.stateDetail?.vbkEndorsementTxId,
            transactionFee: operation?.stateDetail?.vbkEndorsementTxFee,
            transactionFeePerByte:
              operation?.stateDetail?.vbkEndorsementTxFeePerByte,
          }
        );

      case 4:
        return this.translate.instant('ApmOperationState_Done_Block_Of_Proof', {
          contextVbkTokenName: this.operationChain,
          blockOfProofHashHref:
            this.networkExplorerBaseUrl?.blockByHash +
            operation?.stateDetail?.vbkBlockOfProof,
          blockOfProofHash: operation?.stateDetail?.vbkBlockOfProof,
          blockOfProofHeight: operation?.stateDetail?.vbkBlockOfProofHeight,
        });

      case 5:
        return this.translate.instant('ApmOperationState_Done_Proven');

      case 6:
        return this.translate.instant(
          'ApmOperationState_Done_Submitted_Pop_Data',
          {
            operationAtvIdHref:
              this.selectedAltChain?.explorerBaseUrls?.atv +
              operation?.stateDetail?.altAtvId,
            operationAtvId: operation?.stateDetail?.altAtvId,
            operationChainName: this.selectedAltChain?.name,
          }
        );

      case 7:
        return operation?.stateDetail?.expectedRewardBlock &&
          operation?.stateDetail?.publicationDataPayoutInfoDisplay
          ? this.translate.instant('ApmOperationState_Done_Payout_Detected', {
              operationChainName: this.selectedAltChain?.name,
              payoutBlockHeightHref:
                this.selectedAltChain?.explorerBaseUrls?.blockByHash +
                operation?.stateDetail?.expectedRewardBlock,
              payoutBlockHeight: operation?.stateDetail?.expectedRewardBlock,
              addressHref:
                this.selectedAltChain?.explorerBaseUrls?.address +
                operation?.stateDetail?.publicationDataPayoutInfoDisplay,
              address: operation?.stateDetail?.publicationDataPayoutInfoDisplay,
            })
          : this.translate.instant(
              'ApmOperationState_Done_Payout_Detected_No_Instruction',
              { operationChainName: this.selectedAltChain?.name }
            );

      default:
        return '';
    }
  }

  private getCurrentHint(
    operation: OperationDetailResponse,
    index: number
  ): string {
    switch (index) {
      case 3:
        return this.translate.instant(
          'ApmOperationState_Current_Endorsement_Tx_Confirmed',
          { contextVbkTokenName: this.operationChain }
        );

      case 6:
        return this.translate.instant(
          'ApmOperationState_Current_Submitted_Pop_Data',
          { operationChainName: this.selectedAltChain?.name }
        );

      case 7:
        if (!this.hasAllMiningInstructions(operation?.stateDetail)) {
          return this.translate.instant(
            'ApmOperationState_Payout_Detected_No_Instruction'
          );
        }

        return !operation?.stateDetail?.altAtvRequiredConfirmations
          ? this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_No_Confirmation'
            )
          : this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_Confirmation',
              {
                currentConfirmations:
                  operation?.stateDetail?.altAtvCurrentConfirmations || '0',
                requiredConfirmations:
                  operation?.stateDetail?.altAtvRequiredConfirmations,
                operationAtvIdHref:
                  this.selectedAltChain?.explorerBaseUrls?.atv +
                  operation?.stateDetail?.altAtvId,
                operationAtvId: operation?.stateDetail?.altAtvId,
                operationChainName: this.selectedAltChain?.name,
                payoutBlockHeightHref:
                  this.selectedAltChain?.explorerBaseUrls?.blockByHash +
                  operation?.stateDetail?.expectedRewardBlock,
                payoutBlockHeight: operation?.stateDetail?.expectedRewardBlock,
                addressHref:
                  this.selectedAltChain?.explorerBaseUrls?.address +
                  operation?.stateDetail?.publicationDataPayoutInfoDisplay,
                address:
                  operation?.stateDetail?.publicationDataPayoutInfoDisplay,
              }
            );

      default:
        return '';
    }
  }

  private getPendingHint(
    operation: OperationDetailResponse,
    index: number
  ): string {
    switch (index) {
      case 3:
        return this.translate.instant(
          'ApmOperationState_Pending_Endorsement_Tx_Confirmed',
          { contextVbkTokenName: this.operationChain }
        );

      case 6:
        return this.translate.instant(
          'ApmOperationState_Pending_Submitted_Pop_Data',
          { operationChainName: this.selectedAltChain?.name }
        );

      case 7:
        if (!this.hasAllMiningInstructions(operation?.stateDetail)) {
          return this.translate.instant(
            'ApmOperationState_Payout_Detected_No_Instruction'
          );
        }

        return !operation?.stateDetail?.altAtvRequiredConfirmations
          ? this.translate.instant(
              'ApmOperationState_Pending_Payout_Detected',
              {
                operationChainName: this.selectedAltChain?.name,
                payoutBlockHeight: operation?.stateDetail?.expectedRewardBlock,
                addressHref:
                  this.selectedAltChain?.explorerBaseUrls?.address +
                  operation?.stateDetail?.publicationDataPayoutInfoDisplay,
                address:
                  operation?.stateDetail?.publicationDataPayoutInfoDisplay,
              }
            )
          : this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_Confirmation',
              {
                currentConfirmations:
                  operation?.stateDetail?.altAtvCurrentConfirmations || '0',
                requiredConfirmations:
                  operation?.stateDetail?.altAtvRequiredConfirmations,
                operationAtvId: operation?.stateDetail?.altAtvId,
                operationChainName: this.selectedAltChain?.name,
                payoutBlockHeight: operation?.stateDetail?.expectedRewardBlock,
                addressHref:
                  this.selectedAltChain?.explorerBaseUrls?.address +
                  operation?.stateDetail?.publicationDataPayoutInfoDisplay,
                address:
                  operation?.stateDetail?.publicationDataPayoutInfoDisplay,
              }
            );

      default:
        return '';
    }
  }

  private hasAllMiningInstructions(stateDetail: StateDetail): boolean {
    const miningOperations: string[] = [
      'chainIdentifier',
      'endorsedBlockHeight',
      'publicationDataHeader',
      'publicationDataContextInfo',
      'publicationDataPayoutInfo',
      'publicationDataPayoutInfoDisplay',
      'vbkContextBlockHashes',
      'btcContextBlockHashes',
    ];

    return miningOperations.every((key) => stateDetail?.hasOwnProperty(key));
  }

  public getAltChainLogo(key: string): string {
    return `https://cryptoicons.org/api/icon/${
      key.includes('btc') ? 'btc' : key
    }/24`;
  }

  public capitalizeAcronym(key: string): string {
    return key?.length >= 4
      ? `${key.charAt(0)}${key.slice(1).toUpperCase()}`
      : key.toUpperCase();
  }

  public disableMineOperations(): boolean {
    this.mineButtonReady =
      this.isLoadingConfiguration ||
      !this.selectedAltChain?.readyStatus?.isReady ||
      !this.minerStatus?.isReady;

    return this.mineButtonReady;
  }
}
