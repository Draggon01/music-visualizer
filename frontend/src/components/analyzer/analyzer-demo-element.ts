import {LitElement, html, css, PropertyValues, TemplateResult} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {AnalyzerData} from "./analyzerData";

@customElement('analyzer-demo-element')
export class AnalyzerDemoElement extends LitElement {
    static styles = css`
        :host {
            display: block;
            padding: 16px;
        }
    `;

    @property()
    analyzerData?: AnalyzerData;

    cnt: number = 0;

    last: number = 0;

    connectedCallback() {
        super.connectedCallback();
        setInterval(() => {
            console.log("About : " + (this.cnt - this.last) + " FPS")
            this.last = this.cnt
        }, 1000);
    }

    protected updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);
        this.cnt++;
    }

    render() {
        return html`
            <div>
                ${this.cnt}
            </div>
            <div>
                ${this.analyzerData ? this.analyzerData.sampleRate : "no sample data"}
            </div>

            <div style="display:flex; height: 50vh; width: 80vw; background-color: white">
                ${this.showbarsleft(150)}
            </div>
        `;
    }

    private showbarsleft(cnt: number) {
        if (this.analyzerData) {
            let bars: TemplateResult[] = [];

            for (let i = 0; i < cnt; i++) {
                let number = Math.round(this.analyzerData?.leftFFTData[i] * this.analyzerData?.leftFFTData[i]);
                bars.push(html`
                    <div style="background-color: red; height: ${number}px; width: 15px; align-self: end"></div>`)
            }
            return bars;
        }
        return html``
    }
}