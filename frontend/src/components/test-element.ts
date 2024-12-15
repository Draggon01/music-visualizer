import {LitElement, html, css} from 'lit';
import {customElement, property} from 'lit/decorators.js';

@customElement('my-element')
export class MyElement extends LitElement {
    static styles = css`
        :host {
            display: block;
            padding: 16px;
        }
    `;

     @property()
     test = '';

    render() {
        return html`2423423434`;
    }
}