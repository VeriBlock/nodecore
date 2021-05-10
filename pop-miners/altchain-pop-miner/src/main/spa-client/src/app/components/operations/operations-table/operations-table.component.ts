import {
  Component,
  OnInit,
  Input,
  ViewChild,
  SimpleChanges,
  OnChanges,
  EventEmitter,
  Output,
} from '@angular/core';
import {
  trigger,
  state,
  style,
  transition,
  animate,
} from '@angular/animations';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';

import { MinerService } from '@core/services/miner.service';

import { LogsDialogComponent } from '../logs-dialog/logs-dialog.component';
import { OperationSummaryResponse } from '@core/model';
import { OperationStatus, OperationWorkflowState } from '@core/enums';

@Component({
  selector: 'vbk-operations-table',
  templateUrl: './operations-table.component.html',
  styleUrls: ['./operations-table.component.scss'],
  animations: [
    trigger('operationExpand', [
      state('collapsedOperation', style({ height: '0px', minHeight: '0' })),
      state('expandedOperation', style({ height: '*' })),
      transition(
        'expandedOperation <=> collapsedOperation',
        animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')
      ),
    ]),
  ],
})
export class OperationsTableComponent implements OnInit, OnChanges {
  @Input()
  public isLoading = true;

  @Input()
  public totalLines = 0;

  @Input()
  public pageOffset = 0;

  @Input()
  public defaultPageSize = 10;

  @Input()
  public pageSizeOptions: number[] = [10, 25, 100];

  @Input()
  public displayedColumns: string[] = [
    'operationIdColumn',
    'stateColumn',
    'taskColumn',
    'operationStatusColumn',
  ];

  @Input()
  public dataSource = new MatTableDataSource<OperationSummaryResponse>();

  @Input()
  public selectedOperationId: string = null;

  @Input()
  public operationWorkflows = {};

  @Output()
  public pageChange = new EventEmitter<PageEvent>();

  @ViewChild(MatPaginator)
  public paginator: MatPaginator;

  public isLoadingLogs = false;
  public defaultPageIndex = 0;

  constructor(
    private minerService: MinerService,
    private router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog
  ) {}

  public ngOnInit(): void {}

  public ngOnChanges(changes: SimpleChanges): void {
    if (changes['dataSource'] && changes['dataSource'].currentValue) {
      this.dataSource.paginator = this.paginator;
    }
  }

  public selectOperation(operation: OperationSummaryResponse) {
    this.selectedOperationId =
      operation?.operationId === this.selectedOperationId
        ? null
        : operation?.operationId;

    this.updateQueryParams();
  }

  private updateQueryParams() {
    const queryParams: Params = {};
    queryParams['selectedOperationId'] = this.selectedOperationId || null;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: queryParams,
      queryParamsHandling: 'merge',
    });
  }

  public pageChangeEvent(event: PageEvent) {
    this.pageChange.emit(event);
  }

  public openLogsDialog(level: string) {
    this.isLoadingLogs = true;
    this.minerService
      .getOperationLogs(this.selectedOperationId, level)
      .subscribe((logs) => {
        this.isLoadingLogs = false;
        const dialogConfig = new MatDialogConfig();
        dialogConfig.data = {
          logs,
          operationId: this.selectedOperationId,
          level: level === 'info' ? '' : level,
        };

        dialogConfig.panelClass = 'logs-dialog';

        this.dialog.open(LogsDialogComponent, dialogConfig);
      });
  }

  public getIcon(status: string): string {
    switch (status) {
      case OperationStatus.DONE:
        return 'check_circle_outline';

      case OperationStatus.FAILED:
        return 'highlight_off';

      default:
        return '';
    }
  }

  public changeOperationRowState(item: OperationSummaryResponse): string {
    return item?.operationId === this.selectedOperationId
      ? 'expandedOperation'
      : 'collapsedOperation';
  }

  public checkStatus(status: string): string {
    switch (status) {
      case OperationStatus.DONE:
        return 'text-success';

      case OperationStatus.ACTIVE:
        return 'text-warning';

      default:
        return 'text-danger';
    }
  }

  public checkOperationStatus(task: string): string {
    if (task.toLowerCase().includes('failed')) return OperationStatus.FAILED;

    if (task.toLowerCase().includes('done')) return OperationStatus.DONE;

    return OperationStatus.ACTIVE;
  }

  public capitalizeFirstLetter = (s: string): string => {
    if (typeof s !== 'string') return '';
    return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
  };

  public getWorkFlowTask = (i: number) => {
    return OperationWorkflowState[i];
  };

  public getClassForStatusRow(task: string): string {
    return !task ? '' : `status-border status-${task.toLowerCase()}`;
  }
}
