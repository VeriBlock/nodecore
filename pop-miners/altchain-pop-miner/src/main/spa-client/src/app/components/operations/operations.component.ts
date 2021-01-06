import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatSelectChange } from '@angular/material/select';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { startWith, switchMap } from 'rxjs/operators';
import { EMPTY, interval } from 'rxjs';

import { ApiService } from '@core/service/api.service';

import { Operation } from '@core/model/operation.model';

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

  public operationsTotalCount: number = 0;
  public selectedOperationId: string;

  public operationsDataSource = new MatTableDataSource<Operation>();

  public pageLimit = 10;
  public pageOffset = 0;

  public operationWorkflows = {};

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router
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
          this.apiService.getOperations(
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
            ? this.apiService.getOperationWorkflow(this.selectedOperationId)
            : EMPTY
        )
      )
      .subscribe((workflow) => {
        this.operationWorkflows[workflow.operationId] = workflow;
      });
  }

  public loadWorkFlow() {
    this.apiService
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

    this.apiService
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
}
