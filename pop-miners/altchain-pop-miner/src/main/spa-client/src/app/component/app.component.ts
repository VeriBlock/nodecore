import {Component, OnInit, ViewChild} from '@angular/core';
import {Location} from '@angular/common';
import {ApiService} from "../service/api.service";
import {Operation, OperationSummaryList} from "../model/operation";
import {EMPTY, empty, interval, of} from "rxjs";
import {catchError, startWith, switchMap} from "rxjs/operators";
import {MineDialogComponent} from "./mine-dialog/mine-dialog.component";
import {LogsDialogComponent} from "./logs-dialog/logs-dialog.component";
import {MatDialog, MatDialogConfig} from "@angular/material/dialog";
import {animate, state, style, transition, trigger} from "@angular/animations";
import {ConfiguredAltchain} from "../model/configured-altchain";
import {MatPaginator, PageEvent} from "@angular/material/paginator";
import {ActivatedRoute} from "@angular/router";

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

  constructor(
  ) {
  }

  ngOnInit() {
  }

}


