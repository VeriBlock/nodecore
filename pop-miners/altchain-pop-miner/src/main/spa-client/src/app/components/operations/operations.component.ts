import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatSelectChange } from '@angular/material/select';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { startWith, switchMap } from 'rxjs/operators';
import { EMPTY, interval } from 'rxjs';

import { DataShareService } from '@core/services/data-share.service';
import { ConfigService } from '@core/services/config.service';
import { AlertService } from '@core/services/alert.service';
import { MinerService } from '@core/services/miner.service';

import { CoinConfigurationDialogComponent } from './coin-configuration-dialog/coin-configuration-dialog.component';

import {
  ConfiguredAltchain,
  AutoMineRound,
  OperationSummaryResponse,
  OperationDetailResponse,
  OperationWorkflow,
  OperationWorkflowStage,
} from '@core/model';
import { OperationState, OperationStatus } from '@core/enums';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'vbk-operations',
  templateUrl: './operations.component.html',
  styleUrls: ['./operations.component.scss'],
})
export class OperationsComponent implements OnInit, OnDestroy {
  public form: FormGroup = this.formBuilder.group({
    filter: '',
  });

  public selectedAltChain: ConfiguredAltchain = null;

  public filters: string[] = ['all', 'active', 'completed', 'failed'];
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

  public operationsTotalCount: number = 0;
  public selectedOperationId: string = null;

  public operationsDataSource = new MatTableDataSource<OperationSummaryResponse>();

  public pageLimit = 10;
  public pageOffset = 0;

  public operationWorkflows = {};

  private currentSelectionSubscription: any;

  constructor(
    private dataShareService: DataShareService,
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
    this.currentSelectionSubscription = this.dataShareService.currentAltChain.subscribe(
      (data: ConfiguredAltchain) => {
        this.initValues();
        this.updateQueryParams();
        this.selectedAltChain = data;
      }
    );

    // Get route's query params
    this.route.queryParams.subscribe((params) => {
      this.selectedOperationId = params.selectedOperationId || null;

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
            status: this.form.controls['filter']?.value || 'active',
            limit: this.pageLimit,
            offset: this.pageOffset,
          })
        )
      )
      .subscribe((response) => {
        this.operationsDataSource.data = response.operations.slice();
        this.operationsTotalCount = response.totalCount;
        this.isLoading = false;
      });

    // Check the operation details API every 5 seconds
    interval(5_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.selectedOperationId
            ? // ? this.minerService.getOperation(this.selectedOperationId)
              this.minerService.getOperationWorkflow(this.selectedOperationId)
            : EMPTY
        )
      )
      .subscribe((workflow) => {
        this.operationWorkflows[workflow.operationId] = workflow;

        // .subscribe((data: OperationDetailResponse) => {
        //   this.operationWorkflows[data.operationId] = this.createWorkflowTable(
        //     data
        //   );
      });
  }

  ngOnDestroy(): void {
    this.currentSelectionSubscription.unsubscribe();
  }

  public initValues() {
    this.isLoading = true;
    this.tableLoading = false;
    this.isLoadingConfiguration = false;

    this.operationsTotalCount = 0;
    this.selectedOperationId = null;

    this.operationsDataSource.data = [];

    this.pageLimit = 10;
    this.pageOffset = 0;

    this.operationWorkflows = {};
  }

  public loadWorkFlow() {
    this.minerService
      // .getOperation(this.selectedOperationId)
      .getOperationWorkflow(this.selectedOperationId)
      .subscribe((workflow) => {
        this.operationWorkflows[this.selectedOperationId] = workflow;

        // .subscribe((data: OperationDetailResponse) => {
        //   this.operationWorkflows[
        //     this.selectedOperationId
        //   ] = this.createWorkflowTable(data);
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
    this.tableLoading = true;

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
      this.form.controls['filter']?.value != 'active'
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
        minWidth: '250px',
        maxWidth: '500px',
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

  private createWorkflowTable(
    operation: OperationDetailResponse
  ): OperationWorkflow {
    console.log(operation);

    const data: OperationWorkflow = {
      operationId: operation.operationId,
      stages: [],
    };

    const currentStageNumber: number =
      this.globalOperationStages.findIndex((x) => x === operation.state) || 8;

    this.globalOperationStages.forEach((stage, index) => {
      const eachStage: OperationWorkflowStage = {
        status: this.getOperationStatus(stage, currentStageNumber),
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
    status: string,
    operation: OperationDetailResponse,
    index: number
  ): string {
    switch (status) {
      case OperationStatus.DONE:
        return this.getExtraInformation(operation, index);

      case OperationStatus.CURRENT:
        return this.getCurrentHint(operation, index);

      case OperationStatus.PENDING:
        return this.getPendingHint(operation, index);
    }

    return '';
  }

  private getOperationStatus(
    selectedState: string,
    currentStageNumber: number
  ) {
    const selectedStateNumber: number = this.globalOperationStages.findIndex(
      (x) => x === selectedState
    );

    return selectedStateNumber <= currentStageNumber
      ? 'DONE'
      : selectedStateNumber === currentStageNumber + 1
      ? 'CURRENT'
      : 'PENDING';
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
          operationEndorsedBlockHeight: operation?.endorsedBlockHeight,
        });
      case 2:
        return this.translate.instant(
          'ApmOperationState_Done_Endorsement_Transaction',
          {
            contextVbkTokenName: operation?.chain, // TODO tVBK?
            transactionTxId: operation?.stateDetail?.vbkEndorsementTxId,
            transactionFee: operation?.stateDetail?.vbkEndorsementTxFee,
          }
        );
      case 4:
        return this.translate.instant('ApmOperationState_Done_Block_Of_Proof', {
          contextVbkTokenName: operation?.chain, // TODO tVBK?
          blockOfProofHash: operation?.stateDetail?.vbkBlockOfProof,
          blockOfProofHeight: operation?.stateDetail?.vbkBlockOfProof?.height, // TODO from where?
        });
      case 5:
        return this.translate.instant('ApmOperationState_Done_Proven');
      case 6:
        return this.translate.instant(
          'ApmOperationState_Done_Submitted_Pop_Data',
          {
            operationAtvId: operation?.stateDetail?.altAtvId,
            operationChainName: this.selectedAltChain?.name,
          }
        );
      case 7:
        return operation?.stateDetail?.expectedRewardBlock &&
          operation?.stateDetail?.address
          ? this.translate.instant('ApmOperationState_Done_Payout_Detected', {
              operationChainName: this.selectedAltChain?.name,
              payoutBlockHeight: operation?.stateDetail?.expectedRewardBlock,
              address: null,
            })
          : this.translate.instant(
              'ApmOperationState_Done_Payout_Detected_No_Instruction',
              {
                operationChainName: this.selectedAltChain?.name,
              }
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
          {
            contextVbkTokenName: operation?.chain, // TODO tVBK?
          }
        );
      case 6:
        return this.translate.instant(
          'ApmOperationState_Current_Submitted_Pop_Data',
          {
            operationChainName: this.selectedAltChain?.name,
          }
        );
      case 7:
        const containingBlockHeight = parseInt(
          operation?.stateDetail?.altAtvBlock.split('@')[1] || '0',
          10
        );
        const requiredConfirmations =
          parseInt(operation?.stateDetail?.expectedRewardBlock || '0', 10) -
          containingBlockHeight;

        // if !miningInstruction?.let then ApmOperationState_Payout_Detected_No_Instruction

        return !requiredConfirmations
          ? this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_No_Confirmation'
            )
          : this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_Confirmation',
              {
                currentConfirmations:
                  operation?.stateDetail?.currentConfirmations || '0',
                requiredConfirmations,
                operationAtvId: operation?.stateDetail?.altAtvId,
                operationChainName: this.selectedAltChain?.name,
                payoutBlockHeight: operation?.stateDetail?.expectedRewardBlock,
                address: null,
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
          {
            contextVbkTokenName: operation?.chain, // TODO tVBK?
          }
        );
      case 6:
        return this.translate.instant(
          'ApmOperationState_Pending_Submitted_Pop_Data',
          {
            operationChainName: this.selectedAltChain?.name,
          }
        );
      case 7:
        const containingBlockHeight = parseInt(
          operation?.stateDetail?.altAtvBlock.split('@')[1] || '0',
          10
        );
        const requiredConfirmations =
          parseInt(operation?.stateDetail?.expectedRewardBlock || '0', 10) -
          containingBlockHeight;

        // if !miningInstruction?.let then ApmOperationState_Payout_Detected_No_Instruction

        return !requiredConfirmations
          ? this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_No_Confirmation'
            )
          : this.translate.instant(
              'ApmOperationState_Current_Payout_Detected_Confirmation',
              {
                currentConfirmations:
                  operation?.stateDetail?.currentConfirmations || '0',
                requiredConfirmations,
                operationAtvId: operation?.stateDetail?.altAtvId,
                operationChainName: this.selectedAltChain?.name,
                payoutBlockHeight: null,
                address: null,
              }
            );
      default:
        return '';
    }
  }

  // private getStateFromFailedOperation(operation: OperationDetailResponse): MiningOperationState {
  private getStateFromFailedOperation(operationStateDetail: any) {
    // return {
    // operationStateDetail.atvId != null -> ApmOperationState.SUBMITTED_POP_DATA
    // operationStateDetail.merklePath != null -> ApmOperationState.PROVEN
    // operationStateDetail.blockOfProof != null -> ApmOperationState.BLOCK_OF_PROOF
    // operationStateDetail.endorsementTransaction != null -> ApmOperationState.ENDORSEMENT_TRANSACTION
    // operationStateDetail.miningInstruction != null -> ApmOperationState.INSTRUCTION
    //   else -> ApmOperationState.INITIAL
    // }
  }
}
