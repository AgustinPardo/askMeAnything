package com.ap.ama;

public class PromptForm {
    private String prompt;
    private String action;

    public PromptForm() {
    }

    public PromptForm(String prompt, String action) {
        this.prompt = prompt;
        this.action = action;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
