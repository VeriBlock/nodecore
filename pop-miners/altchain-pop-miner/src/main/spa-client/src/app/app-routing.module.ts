import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {OperationsComponent} from "./component/operations.component";


const routes: Routes = [
  {path: '', redirectTo: 'operations', pathMatch: 'full'},
  {path: 'operations', component: OperationsComponent},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {onSameUrlNavigation: 'reload'})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
