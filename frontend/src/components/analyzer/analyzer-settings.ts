import {LitElement, html, css} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {AnalyzerData} from "./analyzerData";

@customElement('analyzer-settings')
export class AnalyzerSettings extends LitElement {
    static styles = css`
        :host {
            display: block;
        }
    `;

    @property()
    analyzerData: AnalyzerData | undefined = undefined;

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`
            <div>
                Sample Rate: ${this.analyzerData?.sampleRate ? this.analyzerData.sampleRate : 'item not set'}
            </div>
        `;
    }
}