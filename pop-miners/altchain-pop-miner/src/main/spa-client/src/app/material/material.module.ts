import { NgModule } from '@angular/core';

// import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
// import { MatExpansionModule } from '@angular/material/expansion';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
// import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
// import { MatListModule } from '@angular/material/list';

const exportedComponents = [
  MatButtonModule,
  MatButtonToggleModule,
  MatDialogModule,
  // MatExpansionModule,
  MatFormFieldModule,
  MatIconModule,
  // MatInputModule,
  // MatListModule,
  MatPaginatorModule,
  // MatProgressSpinnerModule,
  MatSelectModule,
  MatSnackBarModule,
  MatTableModule,
  MatToolbarModule,
];

@NgModule({
  imports: [exportedComponents],
  exports: [exportedComponents],
})
export class MaterialModule {}
