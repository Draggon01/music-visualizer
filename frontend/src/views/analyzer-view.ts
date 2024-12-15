import {LitElement, html, css} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {AnalyzerData} from "../components/analyzer/analyzerData";
import "../components/analyzer/analyzer-demo-element"
@customElement('analyzer-view')
export class AnalyzerView extends LitElement {
    static styles = css`
        :host {
            display: block;
            background: gray;
            width: 100vw;
            height: 100vh;
            padding: 0;
            margin: 0;
        }
    `;

    @property()
    test?: AnalyzerData;



    socket = new WebSocket("ws://localhost:8080/ws")

    connectedCallback() {
        super.connectedCallback();
        this.socket.onmessage = (event) => {
            if(event.data.toString().includes("{")){
                this.test = JSON.parse(event.data.toString());
            }
        };
    }

    render() {
        return html`
            <div>
                ${this.test?.sampleRate ? this.test.sampleRate : 'item not set'}
            </div>
            <analyzer-demo-element .analyzerData="${this.test}">
                
            </analyzer-demo-element>
        `;
    }
}