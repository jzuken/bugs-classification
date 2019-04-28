# -*- coding: utf-8 -*-

import os
import torch
import pickle
import numpy as np

from torch import nn
from torch import optim

from encoder import Encoder
from decoder import Decoder
from seq2seq import Seq2Seq

from batch_token_iterator import BatchTokenIterator
from loss import loss_with_eos

HIDDEN_SIZE = 512
N_LAYERS = 2
ENC_DROPOUT = 0.5
DEC_DROPOUT = 0.5
EDIT_DIM = 4
VEC_TOKEN_DIM = 300

N_EPOCHS = 10
CLIP = 1

BATCH_SIZE = 50

TRAIN_FOLDER = 'datasets/data/tokens/train'
VALID_FOLDER = 'datasets/data/tokens/valid'
TEST_FOLDER = 'datasets/data/tokens/test'

SAVE_DIR = 'models'
MODEL_SAVE_PATH = os.path.join(SAVE_DIR, 'token_seq_model.pt')

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

if not os.path.isdir(SAVE_DIR):
    os.makedirs(SAVE_DIR)

class Seq2SeqTrain(object):
    def __init__(self, device, edit_dim, hidden_size, n_layers, enc_dropout, dec_dropout, vec_token_dim):
        
        self.device = device
        
        encoder = Encoder(
            device = device
            , input_dim = edit_dim
            , hidden_size = hidden_size
            , n_layers = n_layers
            , dropout = enc_dropout
        )

        decoder = Decoder(
            input_dim = 2 * hidden_size + vec_token_dim
            , hidden_size = hidden_size
            , output_dim = vec_token_dim
            , n_layers = n_layers
            , dropout = dec_dropout
        )

        self.model = Seq2Seq(encoder, decoder, device).to(device)
        self.optimizer = optim.Adam(self.model.parameters())
        
        
    def train(self, train_iterator, val_iterator, loss, clip, n_epochs):
        loss_history = []
        val_history = []
        
        best_val = np.Inf
        
        for epoch in range(n_epochs):
            self.model.train()
    
            epoch_loss = 0.0 
            for i_step, (edit, prev, updated) in enumerate(train_iterator):
    
                edit_gpu = edit.to(self.device)
                prev_gpu = prev.to(self.device)
                updated_gpu = updated.to(self.device).contiguous()
    
                output = self.model(prev_gpu, edit_gpu)
    
                # updated = (seq_len, batch size, token_vocab_size)
                # outputs = (seq_len, batch_size, token_vocab_size)
        
                loss_value = loss(output, updated_gpu)  
    
                self.optimizer.zero_grad()
                loss_value.backward()
                self.optimizer.step()
    
                nn.utils.clip_grad_norm_(self.model.parameters(), clip)
    
                epoch_loss += loss_value
    
                print('Train batch: %i/%i' % (i_step + 1, train_iterator.n_batches), end = '\r')
    
            ave_loss = epoch_loss / i_step
            val_loss = self.compute_loss(val_iterator, loss)
            
            loss_history.append(ave_loss)
            val_history.append(val_loss)
            
            if best_val > val_loss:
                best_val = val_loss
                with open(MODEL_SAVE_PATH, 'wb') as model_file:
                    pickle.dump(self.model, model_file)
            
            print("Train loss: %f, Val loss: %f" % (ave_loss, val_loss))
            
        return loss_history, val_history
    
    def compute_loss(self, val_iterator, loss):
    
        self.model.eval()    
        epoch_loss = 0.0
        
        with torch.no_grad():
            
            for i_step, (edit, prev, updated) in enumerate(val_iterator):
            
                edit_gpu = edit.to(self.device)
                prev_gpu = prev.to(self.device)
                updated_gpu = updated.to(self.device).contiguous()
    
                output = self.model(prev_gpu, edit_gpu)
    
                loss_value = loss(output, updated_gpu)
                
                epoch_loss += loss_value
            
            return epoch_loss / i_step
    
if __name__ == '__main__':
    train_iterator = BatchTokenIterator(
        dir_path = TRAIN_FOLDER
        , batch_size = BATCH_SIZE
    )
    
    valid_iterator = BatchTokenIterator(
        dir_path = VALID_FOLDER
        , batch_size = BATCH_SIZE
    )
    
    test_iterator = BatchTokenIterator(
        dir_path = TEST_FOLDER
        , batch_size = BATCH_SIZE
    )
    
    best_valid_loss = float('inf')
    
    trainer = Seq2SeqTrain(
        device = device
        , edit_dim = EDIT_DIM
        , hidden_size = HIDDEN_SIZE
        , n_layers = N_LAYERS
        , enc_dropout = ENC_DROPOUT
        , dec_dropout = DEC_DROPOUT
        , vec_token_dim = VEC_TOKEN_DIM
    )
        
    trainer.train(
        train_iterator = train_iterator
        , val_iterator = valid_iterator
        , loss = loss_with_eos
        , clip = CLIP
        , n_epochs = N_EPOCHS
    )