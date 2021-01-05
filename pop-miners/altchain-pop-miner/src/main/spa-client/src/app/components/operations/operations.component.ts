import { Component, OnInit } from '@angular/core';
import {
  animate,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { startWith, switchMap } from 'rxjs/operators';
import { EMPTY, interval } from 'rxjs';

import { MineDialogComponent } from './mine-dialog/mine-dialog.component';
import { LogsDialogComponent } from './logs-dialog/logs-dialog.component';

import { ApiService } from '@core/service/api.service';

import { ConfiguredAltchain } from '@core/model/configured-altchain.model';
import { Operation } from '@core/model/operation.model';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatSelectChange } from '@angular/material/select';

@Component({
  selector: 'operations',
  templateUrl: './operations.component.html',
  styleUrls: ['./operations.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0' })),
      state('expanded', style({ height: '*' })),
      transition(
        'expanded <=> collapsed',
        animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')
      ),
    ]),
  ],
})
export class OperationsComponent implements OnInit {
  public form: FormGroup = this.formBuilder.group({
    filter: '',
  });

  public filters: string[] = ['all', 'active', 'completed', 'failed'];
  public isLoading = true;

  configuredAltchains: ConfiguredAltchain[] = [];

  vbkAddress: string;
  vbkBalance: string;

  operations: Operation[] = [];
  operationsTotalCount: number = 0;
  selectedOperationId: string;
  columnsToDisplay = ['operationId', 'chain', 'state', 'task'];

  pageLimit = 10;
  pageOffset = 0;

  operationWorkflows = {};

  trackByOperationId = (index, operation) => operation.operationId;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private route: ActivatedRoute,
    private location: Location,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    // Get route's query params
    this.route.queryParams.subscribe((params) => {
      console.log(params);
      this.selectedOperationId = params.selectedOperationId;
      console.log(params.statusFilter);
      if (params.statusFilter) {
        console.log(params.statusFilter);
        this.form.controls['filter'].patchValue(params.statusFilter);
      }
      if (params.pageLimit) {
        this.pageLimit = params.pageLimit;
      }
      if (params.pageOffset) {
        this.pageOffset = params.pageOffset;
      }
    });

    // Get the configured altchains
    this.apiService
      .getConfiguredAltchains()
      .subscribe((configuredAltchains) => {
        this.configuredAltchains = configuredAltchains.altchains;
      });

    // Check the miner data API every 61 seconds
    interval(61_000)
      .pipe(
        startWith(0),
        switchMap(() => this.apiService.getMinerInfo())
      )
      .subscribe((response) => {
        this.vbkAddress = response.vbkAddress;
        this.vbkBalance = response.vbkBalance / 100_000_000 + ' VBK';
      });

    // Check the operation list API every 2 seconds
    interval(2_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.apiService.getOperations(
            this.form.controls['filter']?.value || 'active',
            this.pageLimit,
            this.pageOffset
          )
        )
      )
      .subscribe((response) => {
        this.operations = response.operations;
        this.operationsTotalCount = response.totalCount;
        this.isLoading = false;
      });

    // Check the operation details API every 5 seconds
    interval(5_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.selectedOperationId
            ? this.apiService.getOperationWorkflow(this.selectedOperationId)
            : EMPTY
        )
      )
      .subscribe((workflow) => {
        this.operationWorkflows[workflow.operationId] = workflow;
      });
  }

  selectOperation(operation: Operation) {
    if (operation.operationId == this.selectedOperationId) {
      this.selectedOperationId = null;
      this.updateDirectionBar();
      return;
    }
    this.apiService
      .getOperationWorkflow(operation.operationId)
      .subscribe((workflow) => {
        this.operationWorkflows[operation.operationId] = workflow;
        this.selectedOperationId = operation.operationId;
        this.updateDirectionBar();
      });
  }

  openMineDialog() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = this.configuredAltchains;
    this.dialog.open(MineDialogComponent, dialogConfig);
  }

  openLogsDialog(level: string) {
    this.apiService
      .getOperationLogs(this.selectedOperationId, level)
      .subscribe((logs) => {
        const dialogConfig = new MatDialogConfig();
        dialogConfig.data = logs;
        this.dialog.open(LogsDialogComponent, dialogConfig);
      });
  }

  changeStatusFilter(event: MatSelectChange) {
    if (!event.value) {
      return;
    }
    this.updateDirectionBar();
    this.refreshOperationList();
  }

  changePage(event: PageEvent) {
    this.pageLimit = event.pageSize;
    this.pageOffset = event.pageIndex * event.pageSize;
    this.updateDirectionBar();
    this.refreshOperationList();
  }

  private refreshOperationList() {
    this.apiService
      .getOperations(
        this.form.controls['filter']?.value || 'active',
        this.pageLimit,
        this.pageOffset
      )
      .subscribe((response) => {
        this.operations = response.operations;
        this.operationsTotalCount = response.totalCount;
      });
  }

  updateDirectionBar() {
    const queryParams = [];
    if (this.selectedOperationId) {
      queryParams.push(`selectedOperationId=${this.selectedOperationId}`);
    }
    if (this.form.controls['filter']?.value != 'active') {
      queryParams.push(`statusFilter=${this.form.controls['filter']?.value}`);
    }
    if (this.pageLimit != 10) {
      queryParams.push(`pageLimit=${this.pageLimit}`);
    }
    if (this.pageOffset > 0) {
      queryParams.push(`pageOffset=${this.pageOffset}`);
    }
    const query = queryParams.join('&');
    this.location.go('operations', query);
  }
}
