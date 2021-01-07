import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatSelectChange } from '@angular/material/select';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { startWith, switchMap } from 'rxjs/operators';
import { EMPTY, interval } from 'rxjs';

import { ConfigService } from '@core/services/config.service';
import { MinerService } from '@core/services/miner.service';

import { CoinConfigurationDialogComponent } from './coin-configuration-dialog/coin-configuration-dialog.component';

import { Operation } from '@core/model/operation.model';
import { AutoMineRound } from '@core/model/config.model';

@Component({
  selector: 'vbk-operations',
  templateUrl: './operations.component.html',
  styleUrls: ['./operations.component.scss'],
})
export class OperationsComponent implements OnInit {
  public form: FormGroup = this.formBuilder.group({
    filter: '',
  });

  public filters: string[] = ['all', 'active', 'completed', 'failed'];
  public isLoading = true;
  public tableLoading = false;
  public isLoadingConfiguration = false;

  public operationsTotalCount: number = 0;
  public selectedOperationId: string;

  public operationsDataSource = new MatTableDataSource<Operation>();

  public pageLimit = 10;
  public pageOffset = 0;

  public operationWorkflows = {};

  constructor(
    private formBuilder: FormBuilder,
    private minerService: MinerService,
    private configService: ConfigService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
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
          this.minerService.getOperations(
            this.form.controls['filter']?.value || 'active',
            this.pageLimit,
            this.pageOffset
          )
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
            ? this.minerService.getOperationWorkflow(this.selectedOperationId)
            : EMPTY
        )
      )
      .subscribe((workflow) => {
        this.operationWorkflows[workflow.operationId] = workflow;
      });
  }

  public loadWorkFlow() {
    this.minerService
      .getOperationWorkflow(this.selectedOperationId)
      .subscribe((workflow) => {
        this.operationWorkflows[this.selectedOperationId] = workflow;
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
      .getOperations(
        this.form.controls['filter']?.value || 'active',
        this.pageLimit,
        this.pageOffset
      )
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
                console.log('save successful');
              });
          }
        });
    });
  }
}
