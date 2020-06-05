import {Component, OnInit} from '@angular/core';
import {ApiService} from "../service/api.service";
import {Operation} from "../model/operation";
import {EMPTY, empty, interval} from "rxjs";
import {startWith, switchMap} from "rxjs/operators";
import {MineDialogComponent} from "./mine-dialog/mine-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {animate, state, style, transition, trigger} from "@angular/animations";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class AppComponent implements OnInit {
  operations: Operation[] = []
  selectedOperationId: string
  columnsToDisplay = ['operationId', 'chain', 'state', 'task']

  operationWorkflows = {}

  trackByOperationId = (index, operation) => operation.operationId;

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog
  ) {
  }

  ngOnInit() {
    // Check the list API every 2 seconds
    interval(2000).pipe(
      startWith(0),
      switchMap(() => this.apiService.getOperations())
    ).subscribe(response => {
      this.operations = response.operations
    })
    // Check the details API every 5 seconds
    interval(5000).pipe(
      startWith(0),
      switchMap(() => this.selectedOperationId ? this.apiService.getOperationWorkflow(this.selectedOperationId) : EMPTY)
    ).subscribe(workflow => {
      // FIXME maybe workflow should contain the id?
      this.operationWorkflows[this.selectedOperationId] = workflow
    })
  }

  selectOperation(operation: Operation) {
    if (operation.operationId == this.selectedOperationId) {
      this.selectedOperationId = null
      return
    }
    this.apiService.getOperationWorkflow(operation.operationId).subscribe(workflow => {
      this.operationWorkflows[operation.operationId] = workflow
      this.selectedOperationId = operation.operationId;
    })
  }

  openMineDialog() {
    this.dialog.open(MineDialogComponent);
  }
}
