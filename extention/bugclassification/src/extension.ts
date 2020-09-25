// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';

// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {

	// Use the console to output diagnostic information (console.log) and errors (console.error)
	// This line of code will only be executed once when your extension is activated
	console.log('Congratulations, your extension "bugclassification" is now active!');

	//  
	const gDiagnosticsCollection = vscode.languages.createDiagnosticCollection("testdiagnostic");
	const gDiagnosticsArray = new Array<vscode.Diagnostic>();


	let cfg = vscode.workspace.getConfiguration('bugclassification');
	// vscode.workspace.workspaceFolders will be undefined if no workspace opened
	// let currentFilePath = vscode.workspace.workspaceFolders == undefined ? "" : vscode.workspace.workspaceFolders[0].uri.fsPath;
	let currentFilePath =  vscode.window.activeTextEditor == undefined ? "" : vscode.window.activeTextEditor.document.fileName;

	let classificationPath = "";
	let defectListPath = "";

	if (cfg.path != undefined || cfg.path != ""){
		classificationPath = cfg.path;
	} else {
		vscode.window.showInformationMessage('Path for classification is not set. Please update it in settings.json');
	}

	if (cfg.defectlist != undefined || cfg.defectlist != ""){
		defectListPath = cfg.defectlist;
	} else {
		vscode.window.showInformationMessage('Path for defect list is not set. Please update it in settings.json');
	}

	// The command has been defined in the package.json file
	// Now provide the implementation of the command with registerCommand
	// The commandId parameter must match the command field in package.json
	let disposable = vscode.commands.registerCommand('bugclassification.analysefile', () => {
		// The code you place here will be executed every time your command is executed

		// Display a message box to the user
		vscode.window.showInformationMessage('Hello World from BugClassification is started!');

		// Current command is "Hello World"

		// settings.json should look something like this:
		// {
		// 	"terminal.integrated.shell.windows": "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
		// 	"window.zoomLevel": 1,
		// 	"bugclassification.path": "C:\\Users\\kWX910209\\Documents\\bugs-classification",
		// 	"bugclassification.defectlist": "C:\\temp\\list_CE12800.txt"
		// }

		// should have paths to bug-classification folder, to defect list and to the current open file
		if (classificationPath != "" && defectListPath != "" && currentFilePath != "") {
			const defaults = {
				cwd: classificationPath,
				env: process.env
			};
	
			const { spawn } = require('child_process');
	
			const java = spawn('java', [
				"-Xmx12G", "-Dfile.encoding=UTF-8", "-jar", 
				classificationPath + "\\build\\libs\\bugs-classification-v1.jar", 
				"suggestion",
				currentFilePath,
				classificationPath + "\\test\\dataset",
				defectListPath,
				classificationPath + "\\test\\dataset\\suggestion",
				// "--verbose=yes"
			]);

	
			java.stdout.on('data', (data: any) => {
				console.log(`stdout: ${data}`);

				if (data.toString().trim() == "Save results") {
					console.log("Calculation completed. Safe to fetch suggestion.json");
					vscode.window.showInformationMessage('Analysis completed');
				}
			});
	
			java.stderr.on('data', (data: any) => {
				console.error(`stderr: ${data}`);
				vscode.window.showInformationMessage('Something went wrong');
			});
			
			java.on('close', (code: any) => {
				console.log(`child process exited with code ${code}`);
				vscode.window.showInformationMessage('Extention Closed');
			});
	
			java.on('error', (err: any) => {
				console.log('Failed to start subprocess.');
				vscode.window.showInformationMessage('Something went wrong 2');
			});
		} else {
			vscode.window.showInformationMessage('Path for classification or defect list is not set or possibly no files open to analyse. Please update it in settings.json');
		}

	});

	let disposable2 = vscode.commands.registerCommand('bugclassification.highlighterrors', () => {
		console.log("Start second command");

		const fs = require('fs');

		let lengthArr: number[] = [];

		if (vscode.window.activeTextEditor != undefined) {
			let docText = vscode.window.activeTextEditor.document.getText();
			let docLines = docText.split("\n");

			docLines.forEach(element => {
				lengthArr.push(element.length);
			});

		}

		fs.readFile(classificationPath + "\\test\\dataset\\suggestion\\suggestions.json", (err: any, data: any) => {
			console.log("File read");
			if (err) throw err;
			let suggestion = JSON.parse(data);
			console.log(suggestion.suggestions[0]);

			if (suggestion.suggestions[0] && suggestion.suggestions[0].items.length > 0) {
				let items = suggestion.suggestions[0].items;
				
				for (let i = 0; i < items.length; i++) {
					if (vscode.window.activeTextEditor != undefined) {

						console.log("Line: " + items[i].line + " Column: " + items[i].column + " End: " + (items[i].column + items[i].length));

						let diag = new vscode.Diagnostic(new vscode.Range(new vscode.Position(items[i].line - 1, items[i].column),new vscode.Position(items[i].line - 1, items[i].column+items[i].length)), items[i].reason, vscode.DiagnosticSeverity.Error);
						gDiagnosticsArray.push(diag);

					}
				}
			}

			if (vscode.window.activeTextEditor) {
				gDiagnosticsCollection.set(vscode.window.activeTextEditor.document.uri, gDiagnosticsArray);
			}
		});

	});

	context.subscriptions.push(disposable);
	context.subscriptions.push(disposable2);
}


// this method is called when your extension is deactivated
export function deactivate() {}
