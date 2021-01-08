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

import { Operation } from '@core/model';

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
  ];

  @Input()
  public dataSource = new MatTableDataSource<Operation>();

  @Input()
  public selectedOperationId: string = null;

  @Input()
  public operationWorkflows = {};

  @Output()
  public pageChange = new EventEmitter<PageEvent>();

  @ViewChild(MatPaginator)
  public paginator: MatPaginator;

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

  public selectOperation(operation: Operation) {
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
    this.minerService
      .getOperationLogs(this.selectedOperationId, level)
      .subscribe((logs) => {
        const dialogConfig = new MatDialogConfig();
        dialogConfig.data = logs;
        this.dialog.open(LogsDialogComponent, dialogConfig);
      });
  }

  public getIcon(status: string) {
    switch (status) {
      case 'DONE':
        return 'check_circle';

      case 'FAILED':
        return 'cancel';

      default:
        return '';
    }
  }

  public changeOperationRowState(item: Operation): string {
    return item?.operationId === this.selectedOperationId
      ? 'expandedOperation'
      : 'collapsedOperation';
  }

  public adjustLetters(task: string): string {
    const wordsAndNumbers: string[] = task.toLowerCase().split('. ');
    const words: string[] = wordsAndNumbers[1].split('_');

    words.forEach((word, index) => {
      words[index] = this.capitalizeFirstLetter(word);
    });

    return `${wordsAndNumbers[0]}. ${words.join(' ')}`;
  }

  public capitalizeFirstLetter(word: string): string {
    return word.charAt(0).toUpperCase() + word.slice(1);
  }
}
