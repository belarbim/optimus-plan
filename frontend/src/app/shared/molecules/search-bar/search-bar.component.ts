import { Component, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, NzInputModule, NzIconModule],
  template: `
    <nz-input-group [nzPrefix]="prefixIcon">
      <input
        nz-input
        [placeholder]="placeholder"
        [(ngModel)]="searchValue"
        (ngModelChange)="onInput($event)"
      />
    </nz-input-group>
    <ng-template #prefixIcon>
      <span nz-icon nzType="search"></span>
    </ng-template>
  `,
})
export class SearchBarComponent {
  @Input() placeholder = 'Search...';
  @Output() searchChange = new EventEmitter<string>();

  searchValue = '';
  private subject = new Subject<string>();

  constructor() {
    this.subject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(val => {
      this.searchChange.emit(val);
    });
  }

  onInput(value: string): void {
    this.subject.next(value);
  }
}
