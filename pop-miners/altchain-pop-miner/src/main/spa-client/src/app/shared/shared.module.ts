import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MaterialModule } from '@material/material.module';

import { ProgressIndicatorComponent } from './components/progress-indicator/progress-indicator.component';

const exportedComponents = [ProgressIndicatorComponent];

@NgModule({
  declarations: [exportedComponents],
  imports: [
    CommonModule,
    MaterialModule,
  ],
  exports: [
    CommonModule,
    MaterialModule,
    exportedComponents,
  ],
})
export class SharedModule {}
